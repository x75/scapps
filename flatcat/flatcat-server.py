from pythonosc.osc_server import AsyncIOOSCUDPServer
from pythonosc.dispatcher import Dispatcher
from pythonosc.udp_client import SimpleUDPClient
import asyncio

import numpy as np
import scipy.sparse as spa

scip = "127.0.0.1"
scport = 9000
scclient = SimpleUDPClient(scip, scport)  # Create supercollider client

# client.send_message("/some/address", 123)   # Send float message
# client.send_message("/some/address", [1, 2., "hello"])  # Send message with int, float and string

proj_out_dim = 15
proj_in_dim = 15 # 12

fv_mean = np.zeros((proj_in_dim,))
fv_var = np.ones((proj_in_dim,))
fv_min = np.zeros((proj_in_dim,))
fv_max = np.ones((proj_in_dim,))

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

alpha = 0.995
beta = 1 - alpha

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

def normalize_mean_var(fv):
    global fv_mean, fv_var
    fv_mean = (alpha * fv_mean) + (beta * fv)
    fv_var = (alpha * fv_var) + (beta * np.square(fv_mean - fv))

    # fv_mean = fv_mean_
    # fv_var = fv_var_
    
    fv = (fv - fv_mean) / fv_var
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

# mat = random_matrix(proj_out_dim, proj_in_dim)
mat = create_matrix_reservoir(proj_out_dim, proj_in_dim, 0.5)
# mat = mat / np.linalg.norm(mat)
# mat = normalize_spectral_radius(mat, 1.0)

def matrix_reload_handler(address, *args):
    global mat
    print(f"{address}: {args}")
    p = 0.5
    if len(args) > 0:
        p = args[0]
    mat = create_matrix_reservoir(proj_out_dim, proj_in_dim, p)

def filter_handler(address, *args):
    print(f"{address}: {args}")

def flatcat_handler(address, *args):
    """handle flatcat osc messages and forward to supercollider"""
    global mat, fv_mean, fv_var
    # fv = np.array(args)[3:] # what?
    fv = np.array(args)
    # print(f"{address}: {fv.shape} {fv}")
    # a = np.random.uniform(0, 1, (12,))

    # fvs.append(fv.tolist())
    
    # fv = normalize_min_max(fv)
    # fv = normalize_mean_var(fv)
    fv = normalize_heuristic(fv)
    # print(f"fv {fv}")

    # projection
    fv = np.dot(mat, fv)
    # x = x * 3
    # print(f"fv {fv}")

    # map onto range [0, 2]
    fv = np.tanh(fv) + 1
    
    # specific amplitude map
    
    # x = x / np.linalg.norm(x)
    # x = x * 2
    # print(f"fv {fv.shape}")
    # print(f"fv {fv}")
    scclient.send_message("/flatcat2nupg", fv.tolist())
    
dispatcher = Dispatcher()
dispatcher.map("/filter", filter_handler)
dispatcher.map("/flatcat", flatcat_handler)
dispatcher.map("/matrix_reload", matrix_reload_handler)

# ip = "127.0.0.1"
ip = "0.0.0.0"
port = 8999

async def loop():
    """Example main loop that only runs for 10 iterations before finishing"""
    # for i in range(10):
    i = 0
    while True: 
        print(f"Loop {i}")
        await asyncio.sleep(1)
        i += 1
        # if i > 120:
        #     print("writing logfile, exiting")
        #     np.save("fvs-15-1.npy", np.array(fvs))
        #     return


async def init_main():
    server = AsyncIOOSCUDPServer((ip, port), dispatcher, asyncio.get_event_loop())
    transport, protocol = await server.create_serve_endpoint()  # Create datagram endpoint and start serving

    await loop()  # Enter main loop of program

    transport.close()  # Clean up serve endpoint


asyncio.run(init_main())
