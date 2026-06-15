package com.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞「语音听写（流式版）」客户端：把整段 PCM 音频转写为文字。
 *
 * 对接讯飞 WebAPI（wss://iat-api.xfyun.cn/v2/iat）。鉴权用 APISecret 对
 * host/date/request-line 做 HMAC-SHA256 签名，密钥仅后端持有，绝不下发前端。
 *
 * 用于「沉浸式语音面试」在微信等不支持浏览器原生 SpeechRecognition 的环境下作答：
 * 前端录制 16k/16bit/单声道 PCM 整段上传，本类流式发给讯飞，聚合最终识别结果返回。
 *
 * 音频要求：raw PCM，采样率 16000，16bit，单声道（format=audio/L16;rate=16000）。
 * 讯飞单次会话时长上限约 60 秒，故按「分段录音」使用，单题回答不宜过长。
 *
 * @author 开发人员
 * @date 2026-06-15
 */
@Component
public class XfyunAsrClient {
    private static final Logger log = LoggerFactory.getLogger(XfyunAsrClient.class);

    private static final String HOST_URL = "https://iat-api.xfyun.cn/v2/iat";
    /** 每帧音频字节数：讯飞建议每 40ms 发送 1280B(=16000*2*0.04) */
    private static final int FRAME_SIZE = 1280;
    private static final int STATUS_FIRST = 0;
    private static final int STATUS_CONTINUE = 1;
    private static final int STATUS_LAST = 2;
    /** 整段识别的最长等待时间（含网络与上游处理） */
    private static final long TIMEOUT_SECONDS = 30;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 把整段 PCM 音频转写为文字。
     *
     * @param appId     讯飞 APPID
     * @param apiKey    讯飞 APIKey
     * @param apiSecret 讯飞 APISecret
     * @param pcm       16k/16bit/单声道 raw PCM 字节
     * @return 识别出的完整文字（去首尾空白）
     * @throws Exception 鉴权失败、上游返回错误码、超时、连接异常等
     */
    public String recognize(String appId, String apiKey, String apiSecret, byte[] pcm) throws Exception {
        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("讯飞 ASR 密钥未配置");
        }
        if (pcm == null || pcm.length < FRAME_SIZE) {
            throw new IllegalStateException("音频为空或过短");
        }

        String wsUrl = buildAuthUrl(apiKey, apiSecret);
        // 识别结果聚合器：处理讯飞 wpgs 动态修正（rpl 替换 / apd 追加）
        Decoder decoder = new Decoder();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>(null);

        WebSocket ws = http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(wsUrl), new Listener(appId, pcm, decoder, done, error))
                .join();

        boolean finished = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            ws.abort();
        } catch (Exception ignore) {
            /* ignore */
        }
        if (!finished) {
            throw new IllegalStateException("讯飞 ASR 识别超时");
        }
        if (error.get() != null) {
            throw new IllegalStateException("讯飞 ASR 失败：" + error.get());
        }
        return decoder.text().trim();
    }

    /** WebSocket 监听器：连上后推送整段音频；逐帧聚合识别结果 */
    private class Listener implements WebSocket.Listener {
        private final String appId;
        private final byte[] pcm;
        private final Decoder decoder;
        private final CountDownLatch done;
        private final AtomicReference<String> error;
        private final StringBuilder buffer = new StringBuilder();

        Listener(String appId, byte[] pcm, Decoder decoder, CountDownLatch done, AtomicReference<String> error) {
            this.appId = appId;
            this.pcm = pcm;
            this.decoder = decoder;
            this.done = done;
            this.error = error;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            // 另起线程分帧推送，避免阻塞 WebSocket 回调线程
            new Thread(() -> {
                try {
                    int offset = 0;
                    boolean first = true;
                    // 音频帧：首帧 status=0，其余 status=1
                    while (offset < pcm.length) {
                        int len = Math.min(FRAME_SIZE, pcm.length - offset);
                        int status = first ? STATUS_FIRST : STATUS_CONTINUE;
                        String audioB64 = Base64.getEncoder().encodeToString(
                                java.util.Arrays.copyOfRange(pcm, offset, offset + len));
                        webSocket.sendText(buildFrame(status, audioB64), true).join();
                        first = false;
                        offset += len;
                        Thread.sleep(40); // 模拟实时采样节奏，符合讯飞要求
                    }
                    // 末帧：空音频 + status=2，标志发送结束
                    webSocket.sendText(buildFrame(STATUS_LAST, ""), true).join();
                } catch (Exception e) {
                    error.set("发送音频失败：" + e.getMessage());
                    done.countDown();
                }
            }, "xfyun-asr-send").start();
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                try {
                    handleMessage(buffer.toString());
                } catch (Exception e) {
                    error.set("解析响应失败：" + e.getMessage());
                    done.countDown();
                }
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable err) {
            error.set(err.getMessage());
            done.countDown();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            done.countDown();
            return null;
        }

        private void handleMessage(String text) throws Exception {
            JsonNode root = mapper.readTree(text);
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                error.set("code=" + code + " " + root.path("message").asText(""));
                done.countDown();
                return;
            }
            JsonNode dataNode = root.path("data");
            JsonNode result = dataNode.path("result");
            if (!result.isMissingNode() && !result.isNull()) {
                int sn = result.path("sn").asInt();
                String pgs = result.path("pgs").asText("");
                JsonNode rg = result.path("rg");
                StringBuilder seg = new StringBuilder();
                for (JsonNode wsNode : result.path("ws")) {
                    for (JsonNode cw : wsNode.path("cw")) {
                        seg.append(cw.path("w").asText(""));
                    }
                }
                decoder.put(sn, seg.toString(), pgs,
                        rg.isArray() && rg.size() == 2 ? new int[]{rg.get(0).asInt(), rg.get(1).asInt()} : null);
            }
            if (dataNode.path("status").asInt() == STATUS_LAST) {
                done.countDown();
            }
        }

        private String buildFrame(int status, String audioB64) {
            // 手工拼 JSON，避免引入额外构造对象；字段值均为已知安全内容
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            if (status == STATUS_FIRST) {
                sb.append("\"common\":{\"app_id\":\"").append(appId).append("\"},");
                sb.append("\"business\":{\"language\":\"zh_cn\",\"domain\":\"iat\",")
                        .append("\"accent\":\"mandarin\",\"dwa\":\"wpgs\",\"ptt\":1},");
            }
            sb.append("\"data\":{")
                    .append("\"status\":").append(status).append(',')
                    .append("\"format\":\"audio/L16;rate=16000\",")
                    .append("\"encoding\":\"raw\",")
                    .append("\"audio\":\"").append(audioB64).append("\"}");
            sb.append('}');
            return sb.toString();
        }
    }

    /** 构建讯飞鉴权 wss URL：用 APISecret 对 host/date/request-line 做 HMAC-SHA256 签名 */
    private String buildAuthUrl(String apiKey, String apiSecret) throws Exception {
        URI uri = URI.create(HOST_URL);
        String host = uri.getHost();
        String path = uri.getPath();

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new java.util.Date());

        String signOrigin = "host: " + host + "\n"
                + "date: " + date + "\n"
                + "GET " + path + " HTTP/1.1";
        Mac mac = Mac.getInstance("hmacsha256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256"));
        String signature = Base64.getEncoder()
                .encodeToString(mac.doFinal(signOrigin.getBytes(StandardCharsets.UTF_8)));
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signature);
        String authB64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        String query = "authorization=" + urlEncode(authB64)
                + "&date=" + urlEncode(date)
                + "&host=" + urlEncode(host);
        return "wss://" + host + path + "?" + query;
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /**
     * 识别结果聚合器：讯飞开启 wpgs 动态修正后，结果按句号 sn 分段，
     * pgs=apd 为追加、pgs=rpl 表示替换 rg[0..rg[1]] 区间内的历史结果。
     * 用有序 map 存每个 sn 的文本，按 sn 顺序拼接。
     */
    private static class Decoder {
        private final TreeMap<Integer, String> segs = new TreeMap<>();
        private final ConcurrentHashMap<Integer, Boolean> deleted = new ConcurrentHashMap<>();

        synchronized void put(int sn, String text, String pgs, int[] rg) {
            if ("rpl".equals(pgs) && rg != null) {
                for (int i = rg[0]; i <= rg[1]; i++) {
                    segs.remove(i);
                }
            }
            segs.put(sn, text);
        }

        synchronized String text() {
            StringBuilder sb = new StringBuilder();
            for (String v : segs.values()) {
                if (v != null) sb.append(v);
            }
            return sb.toString();
        }
    }
}
