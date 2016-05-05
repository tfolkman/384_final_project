FROM kaixhin/caffe

RUN pip install --upgrade pip
RUN pip install flask
RUN pip install gunicorn
RUN pip install tornado
RUN pip install Pillow

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ADD . /usr/src/app
