import pickle
import jieba
import sys
jieba.setLogLevel(0)
import warnings
warnings.filterwarnings('ignore')
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.naive_bayes import MultinomialNB


class RecognizerMail:

    def __init__(self):
        # 加载特征提取器
        self.extractor = pickle.load(open('model/extractor.pkl', 'rb'))
        # 加载算法模型(朴素贝叶斯)
        self.estimator = pickle.load(open('model/estimator.pkl', 'rb'))

    def clean_mail(self, email):
        email = ' '.join(jieba.lcut(email))
        return email

    def predict(self, emails):
        """预测的流程"""
        # 1. 对邮件内容进行清洗
        emails = [self.clean_mail(email) for email in emails]
        # 2. 提取邮件内容的特征
        emails = self.extractor.transform(emails)
        # 3. 算法模型进行邮件预测
        labels = self.estimator.predict(emails)
        labels = ['垃圾邮件' if label=='spam' else '正常邮件' for label in labels]
        return labels

if __name__ == '__main__':
    # 检查是否提供了命令行参数
    if len(sys.argv) < 2:
        print("用法: python rec.py \"邮件内容\"")
    else:
        # 获取命令行传入的第一个参数作为邮件内容
        my_email = sys.argv[1]
        
        recognizer = RecognizerMail()
        result = recognizer.predict([my_email])
        print(f"{result[0]}")
# 以下是使用模板
# python RecognizerMail.py 'Received: from mail.example.com ([192.168.1.10]) by mx.example.com; Mon, 15 Aug 2005 09:00:00 +0800 (CST) Message-ID: <test123@mail.example.com> From: \"王经理\" <wang@example.com> To: \"李四\" <li@example.com> Date: Mon, 15 Aug 2005 09:00:00 +0800 Subject: 关于下周的项目进度汇报 李四你好，附件是本周的项目进度表，请查收。如果有问题，我们明天开会讨论。祝好，王经理'