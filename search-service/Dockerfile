FROM python:3.12-slim
WORKDIR /srv
COPY requirements.txt .
ARG PIP_INDEX_URL=https://pypi.org/simple
RUN pip install --no-cache-dir --index-url "$PIP_INDEX_URL" -r requirements.txt
COPY app.py .
# FastEmbed 首次下载中文模型到持久化缓存；不要把模型权重提交到仓库。
ENV FASTEMBED_CACHE_PATH=/models
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
