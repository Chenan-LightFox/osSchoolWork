"""
垃圾邮件识别 Flask API
启动方式: python SpamApi.py (默认端口 5050)
调用方式: POST /check  body: {"subject": "...", "content": "..."}
"""
import os
import sys

# 确保工作目录为脚本所在目录（便于加载相对路径的模型文件）
os.chdir(os.path.dirname(os.path.abspath(__file__)))

from flask import Flask, request, jsonify
from RecognizerMail import RecognizerMail

app = Flask(__name__)

# 启动时加载模型（全局单例）
recognizer = RecognizerMail()
print("[SpamApi] 模型加载完成，服务就绪")


@app.route("/check", methods=["POST"])
def check_spam():
    data = request.get_json(silent=True) or {}
    subject = (data.get("subject") or "").strip()
    content = (data.get("content") or "").strip()
    # 合并主题和正文进行识别
    text = (subject + " " + content).strip()
    if not text:
        return jsonify({"isSpam": False, "label": "正常邮件", "reason": "内容为空"})

    try:
        result = recognizer.predict([text])
        label = result[0]  # "垃圾邮件" 或 "正常邮件"
        return jsonify({
            "isSpam": label == "垃圾邮件",
            "label": label
        })
    except Exception as e:
        return jsonify({"isSpam": False, "label": "识别失败", "error": str(e)}), 500


if __name__ == "__main__":
    port = int(os.environ.get("SPAM_API_PORT", 5050))
    app.run(host="0.0.0.0", port=port, debug=False)
