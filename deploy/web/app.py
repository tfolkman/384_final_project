import os
from flask import Flask, request, redirect, url_for
from werkzeug import secure_filename
from utils import colorize
from flask import send_file
import scipy.misc

UPLOAD_FOLDER = './tmp/'
ALLOWED_EXTENSIONS = set(['jpg'])

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS

@app.route("/", methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        file = request.files['file']
        if file and allowed_file(file.filename):
            file.save(UPLOAD_FOLDER + "bw.jpg")
            color_photo = colorize.run_color(UPLOAD_FOLDER + "bw.jpg")
            scipy.misc.imsave(UPLOAD_FOLDER + "color.jpg", color_photo)
	    return send_file(UPLOAD_FOLDER + "color.jpg", mimetype='image/jpg')
    return """
    <!doctype html>
    <title>Upload new File</title>
    <h1>Upload new File</h1>
    <form action="" method=post enctype=multipart/form-data>
      <p><input type=file name=file>
         <input type=submit value=Upload>
    </form>
    <p>%s</p>
    """ % "<br>".join(os.listdir(app.config['UPLOAD_FOLDER'],))

if __name__ == "__main__":
    app.run(debug=True)
