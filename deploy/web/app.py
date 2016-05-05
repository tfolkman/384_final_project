import os
from flask import Flask, request, redirect, url_for, render_template
from werkzeug import secure_filename
from utils import colorize
from flask import send_file
import scipy.misc
import json
from PIL import Image

UPLOAD_FOLDER = './static/images'
ALLOWED_EXTENSIONS = set(['jpg', 'jpeg', 'bmp'])

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


def allowed_file(filename):
    return '.' in filename and \
           filename.lower().rsplit('.', 1)[1] in ALLOWED_EXTENSIONS


@app.route("/process_photo/", methods=['POST'])
def process_photo():
    request.get_data()
    with open('./static/request.txt', 'w') as outfile:
        outfile.write(request.data)
    file = request.files['bwPhoto']
    if file and allowed_file(file.filename):
        if file.filename.lower().split(".")[1] == "bmp":
            file.save(UPLOAD_FOLDER + "/bw.bmp")
            im = Image.open(UPLOAD_FOLDER + "/bw.bmp")
            im.save(UPLOAD_FOLDER + "/bw.jpg", "JPEG")
        else:
            file.save(UPLOAD_FOLDER + "/bw.jpg")
        color_photo = colorize.run_color(UPLOAD_FOLDER + "/bw.jpg")
        scipy.misc.imsave(UPLOAD_FOLDER + "/color.jpg", color_photo)
        return send_file(UPLOAD_FOLDER + "/color.jpg", mimetype='image/jpg')


@app.route("/", methods=['GET'])
def index():
    return render_template("index.html")


@app.route("/report/", methods=['GET'])
def report():
    return render_template("report.html")


if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True)
