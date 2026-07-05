import pickle
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import accuracy_score
import warnings
warnings.filterwarnings('ignore')


def train():

    # 1. 加载训练数据
    train_data = pickle.load(open('temp/处理训练集.pkl', 'rb'))
    emails = train_data['email']
    labels = train_data['label']

    # 2. 训练特征提取器
    stopwords = [word.strip() for word in open('data/stopwords.txt', encoding='utf-8')]
    extractor = CountVectorizer(stop_words=stopwords, max_features=100000)
    # 稀疏方式存储转换之后的数据，占用的内容就会比较少
    emails = extractor.fit_transform(emails)
    # 特征词的数量
    print('特征词数量:', len(extractor.get_feature_names_out()))
    print(extractor.get_feature_names_out()[:10])

    # 3. 训练算法模型
    estimator = MultinomialNB(alpha=0.01)
    estimator.fit(emails, labels)
    # 查看算法模型在训练集上的准确率
    y_preds = estimator.predict(emails)
    acc = accuracy_score(labels, y_preds)
    print('训练集准确率:', acc)

    # 4. 存储特征提取器和算法模型
    pickle.dump(extractor, open('model/extractor.pkl', 'wb'))
    pickle.dump(estimator, open('model/estimator.pkl', 'wb'))


def evaluate():

    # 1. 加载特征提取器
    extractor = pickle.load(open('model/extractor.pkl', 'rb'))
    # 2. 加载算法模型
    estimator = pickle.load(open('model/estimator.pkl', 'rb'))
    # 3. 加载测试集数据
    test_data = pickle.load(open('temp/处理测试集.pkl', 'rb'))
    emails = test_data['email']
    labels = test_data['label']

    # 4. 测试集的准确率
    emails = extractor.transform(emails)
    y_preds = estimator.predict(emails)
    # 计算准确率
    acc = accuracy_score(labels, y_preds)
    print('测试集准确率:', acc)



if __name__ == '__main__':
    evaluate()