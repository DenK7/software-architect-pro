FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY schema.sql .
COPY app.py .

EXPOSE 8083

CMD ["uvicorn", "app:api", "--host", "0.0.0.0", "--port", "8083"]
