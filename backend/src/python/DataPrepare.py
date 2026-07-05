from collections import Counter
from sklearn.model_selection import train_test_split
import pickle
from joblib import Parallel
from joblib import delayed
import multiprocessing
from tqdm import tqdm  # pip install tqdm
import os


def read_mail_data():

    # index 文件解析
    # labels 存储邮件的标签
    # fnames 存储邮件路径
    labels, fnames = [], []
    for line in open('data/full/index'):
        label, fname = line.strip().split()
        # 将 ../data/000/000 转换为 data/data/000/000
        fname = fname.replace('..', 'data')
        labels.append(label)
        fnames.append(fname)

    # 根据路径读取所有邮件内容
    emails = [open(fname, encoding='gbk', errors='ignore').read().strip() for fname in fnames]
    # 数据分布(在数据集中，不同类别的样本数量有多少)
    print(Counter(labels))
    # 数据分割：一个算法模型训练完成之后需要进行评估。从原始数据中，分出一小部分作为测试集数据，用于模型评估
    # 训练集  测试集
    x_train, x_test, y_train, y_test = train_test_split(emails, labels, test_size=0.2, random_state=22)
    print(Counter(y_train), Counter(y_test))

    # 存储相关数据
    pickle.dump({'email': x_train, 'label': y_train}, open('temp/原始训练集.pkl', 'wb'))
    pickle.dump({'email': x_test,  'label': y_test},  open('temp/原始测试集.pkl', 'wb'))


def clean_email(email):

    import jieba
    jieba.setLogLevel(0)

    # 做分词处理
    email = ' '.join(jieba.lcut(email))
    # 繁简体转换
    # 去除非中文字符
    # ...

    return email


def process_email(emails, labels):

    result_emails = []
    result_labels = []

    # 创建进度条对象
    progress = tqdm(total=len(labels), desc='数据处理进度')
    # 遍历所有的邮件内容，并进行处理
    for email, label in zip(emails, labels):
        email = clean_email(email)
        # 更新进度
        progress.update()
        if len(email) == 0:
            continue
        result_emails.append(email)
        result_labels.append(label)
    # 销毁进度条对象
    progress.close()

    return {'email': result_emails, 'label': result_labels}



def process_email_parallel(emails, labels, cpu_cnt=None):

    # 1. 分配任务
    worker_count = cpu_cnt if cpu_cnt is not None else multiprocessing.cpu_count()
    emails_count = len(labels)
    every_worker_count = int(emails_count / worker_count)
    task_range = list(range(0, emails_count + 1, every_worker_count))

    # 2. 创建并发对象
    parallel = Parallel(n_jobs=worker_count)

    # 3. 创建并发任务
    def task(s, e):
        # 3.1 截取任务需要处理的区间的数据
        my_emails = emails[s: e]
        my_labels = labels[s: e]
        # 3.2 开始处理邮件数据
        result_emails = []
        result_labels = []
        progress = tqdm(total=len(my_labels), desc='进程 %5d' % os.getpid())
        for email, label in zip(my_emails, my_labels):
            email = clean_email(email)
            # 更新进度条
            progress.update()
            if len(email) == 0:
                continue
            result_emails.append(email)
            result_labels.append(label)
        # 销毁进度条
        progress.close()

        return {'email': result_emails, 'label': result_labels}

    tasks = []
    for s, e in zip(task_range[:-1], task_range[1:]):
        my_task = delayed(task)(s, e)
        tasks.append(my_task)

    # 4. 执行合并结果
    # results 是一个列表，每一个元素就是每一个任务的返回内容
    results = parallel(tasks)
    clean_emails = []
    clean_labels = []
    for result in results:
        clean_emails.extend(result['email'])
        clean_labels.extend(result['label'])

    print('处理之后的数据量:', len(clean_labels))

    return {'email': clean_emails, 'label': clean_labels}


def prepare_email_data():
    # 1. 读取相应的原始数据
    train_data = pickle.load(open('temp/原始训练集.pkl', 'rb'))
    test_data = pickle.load(open('temp/原始测试集.pkl', 'rb'))

    # 2. 处理其中的每一封邮件
    train_data = process_email_parallel(train_data['email'], train_data['label'])
    test_data =  process_email_parallel(test_data['email'], test_data['label'])

    # 3. 存储处理后邮件数据
    pickle.dump(train_data, open('temp/处理训练集.pkl', 'wb'))
    pickle.dump(test_data,  open('temp/处理测试集.pkl', 'wb'))


if __name__ == '__main__':
    # read_mail_data()
    prepare_email_data()