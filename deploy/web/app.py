import os
from flask import Flask, request, redirect, url_for, render_template
from werkzeug import secure_filename
from utils import colorize
from flask import send_file
import scipy.misc
import json

UPLOAD_FOLDER = './static/images'
ALLOWED_EXTENSIONS = set(['jpg', 'jpeg'])

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


def allowed_file(filename):
    return '.' in filename and \
           filename.lower().rsplit('.', 1)[1] in ALLOWED_EXTENSIONS


@app.route("/process_photo/", methods=['POST'])
def process_photo():
    file = request.files['bwPhoto']
    if file and allowed_file(file.filename):
        file.save(UPLOAD_FOLDER + "/bw.jpg")
        color_photo = colorize.run_color(UPLOAD_FOLDER + "/bw.jpg")
        scipy.misc.imsave(UPLOAD_FOLDER + "/color.jpg", color_photo)
        return send_file(UPLOAD_FOLDER + "/color.jpg", mimetype='image/jpg')


@app.route("/", methods=['GET'])
def index():
    return render_template("index.html")

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True)
