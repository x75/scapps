"""Small example OSC server

This program listens to several addresses, and prints some information about
received packets.


Searchers
- greedy max
- gradient estimate
- cma-es
- hpo

"""
import threading, signal, time, sys
import argparse
import math, random

from pythonosc import dispatcher
from pythonosc import osc_server

from pythonosc import osc_message_builder
from pythonosc import udp_client

import numpy as np

map1 = {
    10: 'amp',
    11: 'se',
}

def print_volume_handler(unused_addr, args, volume):
    print("[{0}] ~ {1}".format(args[0], volume))

def print_compute_handler(unused_addr, args, volume):
    try:
        print("[{0}] ~ {1}".format(args[0], args[1](volume)))
    except ValueError: pass

class Searcher(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.isrunning = True
        self.cnt_main = 0
        self.loop_time = 0.1
        
        self.bufsize = 100

        self.freq = 440
        self.freq_ = np.ones((self.bufsize, 1)) * self.freq
        self.freq_[0,0] = self.freq
        self.coef = {'amp': 0.9, 'se': 0.9}
        self.avg = {'amp': 0, 'se': 0}
        self.max = {'amp': 0, 'se': 0}
        self.mes_ = {'amp': np.zeros((self.bufsize, 1)), 'se': np.zeros((self.bufsize, 1))}
        self.max_leak = {'amp': 0, 'se': 0}

        self.cnt = 0
        
        self.handlers = {
            'tr': self.handlers_tr
        }
        
        self.client = udp_client.SimpleUDPClient('127.0.0.1', 57120)

        self.client.send_message("/gen", [1, 'active', 1.0])
        self.client.send_message("/mes", [1, 'active', 1.0])
        self.client.send_message("/mes", [2, 'active', 1.0])
        self.client.send_message("/trig_sync", [1])

    def run(self):
        while self.isrunning:
            # print('%s run' % self.__class__.__name__)
            obj = 'amp'
            # obj = 'se'
            
            if self.mes_[obj][0,0] > self.avg[obj]:
                print('better: freq = %s' % ( self.freq_[0,0]))

                # update model
                # self.freq = self.freq_[1,0]
                self.freq = np.mean(self.freq_[1:3,0])
                # new sample
                # self.freq_[0,0] = self.freq + np.random.normal(0, 10.0)
                self.freq_[0,0] = self.freq + np.random.pareto(1.2)
                self.freq_[0] = self.freq + ((np.random.binomial(1, 0.5) - 0.5) * 2) * np.random.pareto(1.5, self.freq_[0].shape) #  * self.sigma_expl
            # timeout 10 sec boredom
            elif self.cnt % 10 == 0:
                # self.freq_ = np.roll(self.freq_, 1, axis=0)
                self.freq_[0,0] = self.freq + np.random.normal(0, 10.0)
            else:
                self.freq_[0,0] = self.freq_[1,0]

            # new eval
            self.client.send_message("/gen", [1, "freq", self.freq_[0,0]])
            # roll data
            self.freq_ = np.roll(self.freq_, 1, axis=0)

            self.cnt += 1
            time.sleep(self.loop_time)
            
    def handlers_tr(self, *args):
        # print('args = %s' % (args,))
        if args[4] < 100:
            self.handlers_mes(*args)
        else:
            self.handlers_gen(*args)

    def handlers_gen(self, *args):
        addr = args[0]
        # client = args[1][0]
        nid = args[2]
        tid = args[3]
        tval = args[4]
        # self.freq = np.roll(self.freq, 1, axis=0)
        # self.freq[0,0] = tval

    def handlers_mes(self, *args):
        addr = args[0]
        # client = args[1][0]
        nid = args[2]
        tid = args[3]
        tval = args[4]
        # print('args = %s, %s' % (tid, tval))
        # print('kwargs = %s' % (kwargs))

        if map1[tid] == 'amp':
            self.mes_['amp'] = np.roll(self.mes_['amp'], 1, axis=0)
            self.mes_['amp'][0,0] = tval
            self.avg['amp'] = self.coef['amp'] * self.avg['amp'] + (1 - self.coef['amp']) * tval
            print('handlers_mes: tid = %s, tval = %s, tavg = %s' % (map1[tid], tval, self.avg['amp']))

        elif map1[tid] == 'se':
            self.mes_['se'] = np.roll(self.mes_['se'], -1, axis=0)
            self.mes_['se'][0,0] = tval
            self.avg['se'] = self.coef['se'] * self.avg['se'] + (1 - self.coef['se']) * tval
            print('handlers_mes: tid = %s, tval = %s, tavg = %s' % (map1[tid], tval, self.avg['se']))
            
        # self.cnt += 1
            
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--ip",
        default="127.0.0.1", help="The ip to listen on")
    parser.add_argument(
        "--port",
        type=int, default=5005, help="The port to listen on")
    args = parser.parse_args()

    searcher = Searcher()
    searcher.start()
  
    dispatcher = dispatcher.Dispatcher()
    dispatcher.map("/filter", print)
    dispatcher.map("/tr", searcher.handlers['tr'])
    dispatcher.map("/volume", print_volume_handler, "Volume")
    dispatcher.map("/logvolume", print_compute_handler, "Log volume", math.log)


    def handler(signum, frame):
        print ('Signal handler called with signal', signum)
        # al.savelogs()
        searcher.isrunning = False
        # rospy.signal_shutdown("ending")
        sys.exit(0)
        # raise IOError("Couldn't open device!")
    
    signal.signal(signal.SIGINT, handler)
    
    server = osc_server.ThreadingOSCUDPServer(
        (args.ip, args.port), dispatcher)
    print("Serving on {}".format(server.server_address))
    server.serve_forever()
  
