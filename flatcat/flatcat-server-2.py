from pythonosc.osc_server import AsyncIOOSCUDPServer
from pythonosc.dispatcher import Dispatcher
from pythonosc.udp_client import SimpleUDPClient
import asyncio, sys

import numpy as np
import scipy.sparse as spa
from scipy.interpolate import interp1d
from functools import partial

# ip = "127.0.0.1"
serverip = "0.0.0.0"
serverport = 8999

scip = "127.0.0.1"
scport = 9000
scclient = SimpleUDPClient(scip, scport)  # Create supercollider client

# client.send_message("/some/address", 123)   # Send float message
# client.send_message("/some/address", [1, 2., "hello"])  # Send message with int, float and string

proj_out_dim = 15
proj_in_dim = 15 # 12

fv_mean_emp = np.array(
    [
        4.07, 5.64, 5.48,
        29.55, -0.06, 0.00, 0.00,
        33.83, 0.00, -0.00, 0.01,
        36.49, 0.07, -0.00, 0.01,
    ]
)
fv_scale_emp = np.array(
    [
        1, 1, 1,
        0.33,  1, 1, 1,
        0.33,  1, 1, 1,
        0.33,  1, 1, 1,
    ]
)
fv_min_emp = np.array(
    [
        4.0,  4.0,  4.0,
        20., -1, -1, -1,
        20., -1, -1, -1,
        20., -1, -1, -1,
    ]
)
fv_max_emp = np.array(
    [
        6,  6,  6,
        60,  1, 1, 1,
        60,  1, 1, 1,
        60,  1, 1, 1,
    ]
)



fvs = []
loop_cnt = 0

alpha = 0.9
beta = 1 - alpha

def make_random_transfer_func():
    x = np.concatenate(([-1], np.random.uniform(-1, 1, 8), [1]))
    y = np.random.uniform(0, 1, 10)
    f = interp1d(x, y, kind="quadratic")

    # xnew = np.arange(0, 9, 0.1)
    xnew = np.arange(-1, 1, 0.01)
    ynew = f(xnew)   # use interpolation function returned by `interp1d`
    # ynew = ynew - np.min(ynew)
    ymin = np.min(ynew)
    ymax = np.max(ynew - ymin)
    # ynew = np.tanh(f(xnew))   # use interpolation function returned by `interp1d`
    def g(ymin, ymax, f, x):
        return ((f(x) - ymin) / ymax) * 2 - 1

    h = partial(g, ymin, ymax, f)
    return h

def normalize_min_max(fv):
    global fv_min, fv_max
    # fv = np.atleast_2d(fv)
    print(f"fv.shape {fv.shape}")
    print(f"fv_min.shape {fv_min.shape}")
    print(f"fv_max.shape {fv_max.shape}")
    fv_min_ = np.vstack((fv, fv_min))
    fv_max_ = np.vstack((fv, fv_max))
    fv_min = fv_min_.min(axis=0)
    fv_max = fv_max_.max(axis=0)

    print(f"fv_min {fv_min}")
    print(f"fv_max {fv_max}")
    
    fv = fv - fv_mean
    fv = fv / (fv_max - fv_min)
    # fv = fv + 0.5
    print(f"fv {fv}")
    return fv

def normalize_mean_var(fv, fv_mean, fv_var):
    # global fv_mean, fv_var
    # fv_mean = (alpha * fv_mean) + (beta * fv)
    # fv_var = (alpha * fv_var) + (beta * np.square(fv_mean - fv))

    print(f"fv_mean {fv_mean}")
    print(f"fv_var {fv_var}")
    
    # fv_mean = fv_mean_
    # fv_var = fv_var_
    
    fv = (fv - fv_mean) / (fv_var * 5)
    print(f"fv {fv}")    
    return fv

def normalize_heuristic(fv):
    """normalize via empirical limits"""
    # remove mean
    fv = fv - fv_mean_emp
    # divide by max magnitude
    fv = fv * fv_scale_emp
    # soft clip using tanh
    fv = np.tanh(fv)
    return fv

def random_matrix(rows, cols):
    mat = np.random.uniform(-1, 1, (rows, cols))
    return mat

def create_matrix_reservoir(N, M, p=0.1):
    """Create an NxN reservoir recurrence matrix with density p"""
    M = spa.rand(N, M, p)
    M = M.todense()
    tmp_idx = M != 0
    tmp = M[tmp_idx]
    tmp_r = np.random.normal(0, 1, size=(tmp.shape[1],))
    M[tmp_idx] = tmp_r
    # print "type(M)", type(M)
    # print "M.shape", M.shape
    # M = np.array(M * self.g * self.scale)
    return np.array(M).copy()
    # return spa.bsr_matrix(M)
    
def normalize_spectral_radius(M, g):
    """Normalize the spectral radius of matrix M to g"""
    # compute eigenvalues
    [w,v] = np.linalg.eig(M)
    # get maximum absolute eigenvalue
    lae = np.max(np.abs(w))
    # print "lae pre", lae
    # normalize matrix
    M /= lae
    # scale to desired spectral radius
    M *= g
    return M

class oscserv(object):
    def __init__(self, serverip, serverport, in_dim, out_dim):
        self.serverip = serverip
        self.serverport = serverport
        self.in_dim = in_dim
        self.out_dim = out_dim
        self.p = 0.5
        # mat = random_matrix(proj_out_dim, proj_in_dim)
        self.mat = create_matrix_reservoir(self.out_dim, self.in_dim, self.p)
        # mat = mat / np.linalg.norm(mat)
        # mat = normalize_spectral_radius(mat, 1.0)

        self.fv_mean = np.zeros((self.in_dim,))
        self.fv_var = np.ones((self.in_dim,))
        self.fv_min = np.zeros((self.in_dim,))
        self.fv_max = np.ones((self.in_dim,))

        self.transfers = [make_random_transfer_func() for _ in range(self.out_dim)]
        print(f"transfer funcs {self.transfers}")
        
        self.dispatcher = Dispatcher()
        self.dispatcher.map("/filter", self.filter_handler)
        self.dispatcher.map("/flatcat", self.flatcat_handler)
        self.dispatcher.map("/matrix_reload", self.matrix_reload_handler)

    def matrix_reload_handler(self, address, *args):
        # global mat
        print(f"{address}: {args}")
        p = 0.5
        if len(args) > 0:
            p = args[0]
        self.mat = create_matrix_reservoir(proj_out_dim, proj_in_dim, p)

    def filter_handler(self, address, *args):
        print(f"{address}: {args}")

    def flatcat_handler(self, address, *args):
        """handle flatcat osc messages and forward to supercollider"""
        # global mat, fv_mean, fv_var
        # fv = np.array(args)[3:] # what?
        fv = np.array(args)
        print(f"{address}: {fv.shape} {fv}")
        # a = np.random.uniform(0, 1, (12,))

        # input stats
        self.fv_mean = (alpha * self.fv_mean) + (beta * fv)
        self.fv_var = (alpha * self.fv_var) + (beta * np.sqrt(np.square(self.fv_mean - fv)))
        
        # fvs.append(fv.tolist())
        
        # fv = normalize_min_max(fv)
        # fv = normalize_mean_var(fv, self.fv_mean, self.fv_var)
        fv = normalize_heuristic(fv)
        # print(f"fv {fv}")
        
        # projection
        fv = np.dot(self.mat, fv)
        # x = x * 3
        # print(f"fv {fv}")
        
        # # map onto range [0, 2]
        # fv = np.tanh(fv) + 1

        # squelch
        fv = np.tanh(fv)
        
        # apply array of random transfer functions to each element
        fv_ = []
        for i, _ in enumerate(fv):
            __ = self.transfers[i](_) + 1
            fv_.append(__)
        fv = np.array(fv_)
            
        # self.fv_mean = (alpha * self.fv_mean) + (beta * fv)
        # self.fv_var = (alpha * self.fv_var) + (beta * np.square(self.fv_mean - fv))
        
        # specific amplitude map
        
        # x = x / np.linalg.norm(x)
        # x = x * 2
        # print(f"fv {fv.shape}")
        # print(f"fv {fv}")
        scclient.send_message("/flatcat2nupg", fv.tolist())

async def loop(oscserv):
    """Example main loop that only runs for 10 iterations before finishing"""
    # for i in range(10):
    i = 0
    while True: 
        print(f"loop {i} fv mean")
        sys.stdout.write(f"  ")
        for _ in oscserv.fv_mean:
            sys.stdout.write(f"{_:>.1f}, ")
        print(f"")
        await asyncio.sleep(1)
        i += 1
        # if i > 120:
        #     print("writing logfile, exiting")
        #     np.save("fvs-15-1.npy", np.array(fvs))
        #     return


async def init_main():
    oscserv_inst = oscserv(serverip, serverport, proj_in_dim, proj_out_dim)
    server = AsyncIOOSCUDPServer(
        (serverip, serverport),
        oscserv_inst.dispatcher,
        asyncio.get_event_loop()
    )
    transport, protocol = await server.create_serve_endpoint()  # Create datagram endpoint and start serving

    await loop(oscserv_inst)  # Enter main loop of program

    transport.close()  # Clean up serve endpoint


asyncio.run(init_main())
