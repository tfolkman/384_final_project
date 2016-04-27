FROM kaixhin/caffe

RUN pip install --upgrade pip

RUN pip install flask

# add our project
ADD . /

# expose the port for the API
EXPOSE 5000

# run the API 
CMD [ "python", "/api.py" ]
