"""resonance finder
"""

import argparse

import numpy as np
import matplotlib.pyplot as plt

from scipy.signal import chirp, spectrogram

def main_single(args):
    # print "args", args
    plt.ion()
    # system: very simple resonance model mit one delay line per dimension of space

    # space dimension
    dim_space = 2 # standard
    # state dimension
    dim_state = 1 # monoaural microphone
    # motor dimension
    dim_motor = 1 # monoaural speaker

    delay_max = 200
    delay_buf = np.zeros((delay_max, 1)) # dim_state))
    # delay_tap = np.array((13, 17, 39))
    # delay_tap = np.array((20, 25))
    delay_tap = np.array((20, 25, 39, 120, 137, 67))
    # W_i = np.zeros(())

    # exploration: sweep, uniform noise, ...
    sr = 1e4
    numsec = 10
    numstep = int(sr * numsec)
    T = np.linspace(0, 1, numstep)  #

    # # input
    # X = chirp(T, 0, 1.0, sr/2) #
    # Y = np.zeros((numstep, 1))

    # for i, t in enumerate(T):
    #     # update delay lines
    #     delay_buf[0,0] = X[i]
    #     delay_buf = np.roll(delay_buf, shift = 1, axis = 0)
        
    #     # update state from taps (end of line)
    #     Y[i,0] = X[i] + np.sum(delay_buf[delay_tap,0] * 0.96)

    # Xf, Xt, Xspec = spectrogram(X.T, fs = sr)
    # Yf, Yt, Yspec = spectrogram(Y.T, fs = sr)
    # print "X", Xf.shape, Xt.shape, Xspec.shape
    # print "Y", Yf.shape, Yt.shape, Yspec.shape
        
    # # output
    # fig = plt.figure()
    # ax1 = fig.add_subplot(2,2,1)
    # ax1.plot(X)
    # ax2 = fig.add_subplot(2,2,3)
    # ax2.pcolor(Xt, Xf, Xspec)
    # ax3 = fig.add_subplot(2,2,2)
    # ax3.plot(Y)
    # ax4 = fig.add_subplot(2,2,4)
    # ax4.pcolor(Yt, Yf, Yspec[0])
    # plt.draw()


    # interactive
    param_f = np.random.uniform(0, sr/2.0)
    A = np.zeros((numstep, 1))
    DT_ = np.zeros((numstep, 1))
    X = np.zeros((numstep, 1))
    Y = np.zeros((numstep, 1))
    A_var = np.zeros((numstep, 1))
    X_var = np.zeros((numstep, 1))
    Y_var = np.zeros((numstep, 1))

    eta = 0.5
    
    t_ = 0.0
    dt_ = (param_f/sr)*np.pi
    updatecnt = 0
    for i, t in enumerate(T):
        if i % 100 == 0:
            A[i,0] = dt_ + np.random.uniform(-1e-2, 1e-2)
        else:
            A[i,0] = A[i-1,0]

        DT_[i,0] = dt_
            
        # t_ += dt_
        t_ += A[i,0]
        X[i,0] = np.sin(t_)
        
        # update delay lines
        delay_buf[0,0] = X[i,0]
        delay_buf = np.roll(delay_buf, shift = 1, axis = 0)
        
        # update state from taps (end of line)
        Y[i,0] = X[i] + np.sum(delay_buf[delay_tap,0] * 0.96)

        if i < 100: continue

        X_var[i,0] = np.var(X[i-100:i])
        Y_var[i,0] = np.var(Y[i-100:i])

        # if np.sum(Y_var[i-10:i,0]) > np.sum(Y_var[i-110:i-100,0]):
        #     dt_ += eta * (A[i,0] - dt_)

        if i % 100 == 0:
            # print "resfind[%d] checking var" % (i,)
            # if Y_var[i-1,0] > Y_var[i-101,0]:
            if (Y_var[i-1,0] - Y_var[i-101,0]) > 0.001:
                print "resfind[%d] updating dt_ = %f" % (i, dt_)
                dt_ += eta * (A[i-1,0] - dt_)
        else:
            pass

        if i < 2000: continue

        dvar_long = np.abs(np.mean(Y_var[i-1000:i,0]) - np.mean(Y_var[i-2000:i-1000,0]))
        if dvar_long < 1e-2:
        # if i % 10000 == 11000:
            print "dvar_long = %f" % (dvar_long)
            param_f = np.random.uniform(0, sr/2.0)
            # param_f = np.random.uniform(-1e-2, 1e-2)
            dt_ = (param_f/sr)*np.pi
            print "converged / stagnated - reinit dt_ = %f" % (dt_, )

        # measure Y[i,0] with extensiveness N
        # update param_f toward max meas(Y)
    
    Xf, Xt, Xspec = spectrogram(X.T, fs = sr)
    Yf, Yt, Yspec = spectrogram(Y.T, fs = sr)
    
    fig2 = plt.figure()
    ax1 = fig2.add_subplot(4,2,1)
    ax1.plot(X)
    ax2 = fig2.add_subplot(4,2,3)
    ax2.plot(X_var)
    ax3 = fig2.add_subplot(4,2,5)
    ax3.pcolor(Xt, Xf, Xspec[0])
    ax4 = fig2.add_subplot(4,2,2)
    ax4.plot(Y)
    ax5 = fig2.add_subplot(4,2,4)
    ax5.plot(Y_var)
    ax6 = fig2.add_subplot(4,2,6)
    ax6.pcolor(Yt, Yf, Yspec[0])
    ax8 = fig2.add_subplot(4,2,8)
    ax8.plot(DT_)
    plt.draw()

    plt.ioff()
    
    plt.show()
        

    # measure input  power
    # measure output power
    # update freq params towards i/o gain gradient
    
    # sys = 

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-m', '--mode', type=str, default='single')

    args = parser.parse_args()

    if args.mode in ['single']:
        main_single(args)
    else:
        print 'Unknown mode %s' % (args.mode,)
