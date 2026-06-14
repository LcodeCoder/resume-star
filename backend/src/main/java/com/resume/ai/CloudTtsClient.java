package com.resume.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 云端语音合成客户端（文字转语音 TTS）
 *
 * 对接 hewoyi 语音合成接口，密钥仅后端持有：
 *  1) GET {@code .../audio/speech?key=&type=speech&text=&voice=&speed=&model=} → 返回内嵌 &lt;audio&gt; 的 HTML，
 *     其中 {@code <source src="...">} 指向上游真实音频地址；
 *  2) 再请求该音频地址，得到 audio/mpeg 二进制，回流给前端。
 *
 * 失败（密钥失效、限频、解析不到地址）抛异常，由调用方降级到浏览器本地 TTS。
 *
 * @author 开发人员
 * @date 2026-06-15
 */
@Component
public class CloudTtsClient {
    private static final Logger log = LoggerFactory.getLogger(CloudTtsClient.class);

    private static final String ENDPOINT = "https://api.hewoyi.com/api/ai/audio/speech";
    /** 从返回 HTML 里提取 <source src="..."> 的真实音频地址 */
    private static final Pattern SRC = Pattern.compile("src=\"([^\"]+)\"");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 合成语音并返回 mp3 字节。
     *
     * @param key   API 密钥（后端配置）
     * @param text  要合成的文字
     * @param voice 音色 ID，如 zh-CN-XiaoxiaoNeural
     * @param speed 语速，如 "1.0"
     * @param hd    是否高音质（tts-1-hd）
     * @return mp3 二进制
     */
    public byte[] synthesize(String key, String text, String voice, String speed, boolean hd) throws Exception {
        String model = hd ? "tts-1-hd" : "tts-1";
        String url = ENDPOINT
                + "?key=" + enc(key)
                + "&type=speech"
                + "&text=" + enc(text)
                + "&voice=" + enc(voice)
                + "&speed=" + enc(speed)
                + "&model=" + model;

        HttpResponse<String> page = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = page.body() == null ? "" : page.body();
        Matcher m = SRC.matcher(body);
        if (!m.find()) {
            // 没有音频地址，多半是 JSON 错误（限频 / 密钥失效 / 参数错误）
            throw new IllegalStateException("云端 TTS 未返回音频地址：" + truncate(body, 160));
        }
        String audioUrl = m.group(1).replace("&amp;", "&");

        HttpResponse<byte[]> audio = http.send(
                HttpRequest.newBuilder(URI.create(audioUrl)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        byte[] bytes = audio.body();
        if (bytes == null || bytes.length < 200) {
            throw new IllegalStateException("云端 TTS 返回音频为空");
        }
        log.debug("云端 TTS 合成成功：voice={}, bytes={}", voice, bytes.length);
        return bytes;
    }

    private String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
