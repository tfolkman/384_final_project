import numpy as np
import matplotlib.pyplot as plt
import caffe
import os
import skimage.color as color
import scipy.ndimage.interpolation as sni
import argparse
import scipy.misc

plt.rcParams['figure.figsize'] = (12, 6)

def run_color(image, image_out):
    caffe.set_mode_cpu()
    net = caffe.Net('colorization_deploy_v0.prototxt', 'colorization_release_v0.caffemodel', caffe.TEST)

    (H_in,W_in) = net.blobs['data_l'].data.shape[2:] # get input shape
    (H_out,W_out) = net.blobs['class8_ab'].data.shape[2:] # get output shape
    net.blobs['Trecip'].data[...] = 6/np.log(10) # 1/T, set annealing temperature
    
    img_rgb = caffe.io.load_image(image)
    img_lab = color.rgb2lab(img_rgb) # convert image to lab color space
    img_l = img_lab[:,:,0] # pull out L channel
    (H_orig,W_orig) = img_rgb.shape[:2] # original image size

    # resize image to network input size
    img_rs = caffe.io.resize_image(img_rgb,(H_in,W_in)) # resize image to network input size
    img_lab_rs = color.rgb2lab(img_rs)
    img_l_rs = img_lab_rs[:,:,0]

    net.blobs['data_l'].data[0,0,:,:] = img_l_rs-50 # subtract 50 for mean-centering
    net.forward() # run network

    ab_dec = net.blobs['class8_ab'].data[0,:,:,:].transpose((1,2,0)) # this is our result
    ab_dec_us = sni.zoom(ab_dec,(1.*H_orig/H_out,1.*W_orig/W_out,1)) # upsample to match size of original image L
    img_lab_out = np.concatenate((img_l[:,:,np.newaxis],ab_dec_us),axis=2) # concatenate with original image L
    img_rgb_out = np.clip(color.lab2rgb(img_lab_out),0,1) # convert back to rgb

    scipy.misc.imsave(image_out, img_rgb_out)


parser = argparse.ArgumentParser()
parser.add_argument("image_file")
parser.add_argument("image_file_out")

if __name__ == '__main__':
    args = parser.parse_args()
    run_color(args.image_file, args.image_file_out)
