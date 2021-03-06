#include "stdafx.h"
#include <opencv2/opencv.hpp> 
#include <iostream>
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>
#include <stdio.h>

using namespace std;
using namespace cv;



int main(int argc, const char *argv[]) {
	if (argc != 4) {
		cout << "usage: " << argv[0] << " </path/to/haar_cascade> </path/to/csv.ext> </path/to/device id>" << endl;
		cout << "\t </path/to/haar_cascade> -- Path to the Haar Cascade for face detection." << endl;
		cout << "\t </path/to/csv.ext> -- Path to the CSV file with the face database." << endl;
		cout << "\t <device id> -- The webcam device id to grab frames from." << endl;
		//  exit(1);
	}
	CascadeClassifier face_cascade;
	CascadeClassifier eyes_cascade;
	String fn = "C:\\Program Files\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt2.xml";
	String fn2 = "C:\\Program Files\\opencv\\sources\\data\\haarcascades\\haarcascade_eye_tree_eyeglasses.xml";
	face_cascade.load(fn);
	eyes_cascade.load(fn2);

	VideoCapture input(0);
	if (!input.isOpened()) { return -1; }

	namedWindow("Mezo", 1);
	Mat f2;
	Mat frame;

	while (true) {

		input >> frame;
		waitKey(10);
		cvtColor(frame, f2, CV_BGR2GRAY);

		equalizeHist(f2, f2);


		 std::vector<Rect> faces;
    Mat frame_gray;
    cvtColor( frame, frame_gray, COLOR_BGR2GRAY );
    equalizeHist( frame_gray, frame_gray );
    //-- Detect faces
    face_cascade.detectMultiScale( frame_gray, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );
    for( size_t i = 0; i < faces.size(); i++ )
    {
        Point center( faces[i].x + faces[i].width/2, faces[i].y + faces[i].height/2 );
        ellipse( frame, center, Size( faces[i].width/2, faces[i].height/2), 0, 0, 360, Scalar( 255, 0, 255 ), 4, 8, 0 );
        Mat faceROI = frame_gray( faces[i] );
        std::vector<Rect> eyes;
        //-- In each face, detect eyes
        eyes_cascade.detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );
        for( size_t j = 0; j < eyes.size(); j++ )
        {
            Point eye_center( faces[i].x + eyes[j].x + eyes[j].width/2, faces[i].y + eyes[j].y + eyes[j].height/2 );
            int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );
            circle( frame, eye_center, radius, Scalar( 255, 0, 0 ), 4, 8, 0 );
        }
    }

		imshow("Mezo", frame);

		waitKey(3);
		char c = waitKey(3);
		if (c == 27) { break; }

	}

	return 0;
}





