#!flask/bin/python
import Robot
import talkey
import subprocess
from flask import Flask
from flask import send_file
from flask import jsonify
app = Flask(__name__)
import RPi.GPIO as GPIO
import time
#from time import sleep
#from picamera import PiCamera
#camera = PiCamera()
LEFT_TRIM   = -5
RIGHT_TRIM  = 0
robot = Robot.Robot(left_trim=LEFT_TRIM, right_trim=RIGHT_TRIM)

#GPIO Mode (BOARD / BCM)
GPIO.setmode(GPIO.BCM)
#set GPIO Pins
GPIO_TRIGGER = 18
GPIO_ECHO = 24

#set GPIO direction (IN / OUT)
GPIO.setup(GPIO_TRIGGER, GPIO.OUT)
GPIO.setup(GPIO_ECHO, GPIO.IN)

tts = talkey.Talkey()

class InvalidUsage(Exception):
    status_code = 400

    def __init__(self, message, status_code=None, payload=None):
        Exception.__init__(self)
        self.message = message
        if status_code is not None:
            self.status_code = status_code
        self.payload = payload

    def to_dict(self):
        rv = dict(self.payload or ())
        rv['message'] = self.message
        return rv


@app.errorhandler(InvalidUsage)
def handle_invalid_usage(error):
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response

@app.route('/')
def index():
    return "Hello, World!"

@app.route('/rover/api/v1.0/cam')
def cam():
#    camera.resolution = (1024, 768)
#    camera.start_preview()
#    # Camera warm-up time
#    camera.capture('foo.jpg')
    return send_file("/dev/shm/mjpeg/cam.jpg",mimetype='image/jpg')

#Server Side:
#/rc/fwd/[seconds]
#/rc/back/[seconds]
@app.route('/rover/api/v1.0/rc/move/<dir>/<seconds>', methods=['GET'])
def rover_move(dir,seconds):
    if (dir == "fwd" or dir == "forward"):
        d1 = distance()
        d2 = distance()
        final_distance = (d1+d2)/2
        #return "i am " + str(final_distance)
        if (final_distance < 30*int(seconds)):
            say("I am " + str(final_distance) + " centimeters from an obstacle, which is too close.")
            raise InvalidUsage("I am " + str(final_distance) + " cm from an obstacle, which is too close for going " + str(30*int(seconds)), status_code=410)
        else:
            robot.forward(150, float(seconds))   # Move forward at speed 150 for 1 second.
    if (dir == "bk" or dir == "backward" or dir == "back"):
        robot.backward(100, float(seconds))
    return dir + " for " + seconds + "seconds"

#/rc/left/[degrees]
#/rc/right/[degrees]
@app.route('/rover/api/v1.0/rc/turn/<dir>/<degrees>', methods=['GET'])
def rover_turn(dir,degrees):
    # convert degrees to seconds.
    seconds = float(degrees) * (0.002)
    if (dir == "left" or dir == "l"):
        robot.left(200, seconds)      # Spin left at speed 200 for 0.5 seconds.
    if (dir == "right" or dir == "r"):
        robot.right(200, seconds)
    return dir + " for " + degrees + "degrees" + str(seconds)

#/see/human:y/n
@app.route('/rover/api/v1.0/see/<name>', methods=['GET'])
def rover_do_you_see(name):
    return name + " in sight? "

#/sonar/ping
@app.route('/rover/api/v1.0/sonar/ping', methods=['GET'])
def rover_ping():
    return "something is " + str(distance()) + " cm away"


#/sonar/ping
@app.route('/rover/api/v1.0/say/<something>', methods=['GET'])
def say_req(something):
    say(something)
    return "something was said: " +something

#/say/whatyousee
@app.route('/rover/api/v1.0/see/what', methods=['GET'])
def see_what():
    cmd = ['java', '-jar', 'image-recognition-test-1.0-SNAPSHOT.jar','/dev/shm/mjpeg/cam.jpg']
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    whatyousee = proc.stdout.read()
    say(whatyousee)
    return whatyousee


@app.route('/rover/api/v1.0/init/speaker', methods=['GET'])
def init_speaker():
    cmd = ['/home/pi/rover-api/init_speaker.sh']
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    result = proc.stdout.read()
    return result


def distance():
	# set Trigger to HIGH
	GPIO.output(GPIO_TRIGGER, True)

	# set Trigger after 0.01ms to LOW
	time.sleep(0.00001)
	GPIO.output(GPIO_TRIGGER, False)

	StartTime = time.time()
	StopTime = time.time()

	# save StartTime
	while GPIO.input(GPIO_ECHO) == 0:
		StartTime = time.time()

	# save time of arrival
	while GPIO.input(GPIO_ECHO) == 1:
		StopTime = time.time()

	# time difference between start and arrival
	TimeElapsed = StopTime - StartTime
	# multiply with the sonic speed (34300 cm/s)
	# and divide by 2, because there and back
	distance = (TimeElapsed * 34300) / 2

	return distance

def say(this):
    tts.say('Oops.')
    tts.say(this)


if __name__ == '__main__':
    app.run(debug=True)

