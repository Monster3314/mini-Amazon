from socket import *
from .models import *
import math


serverName = 'backend'
packagePort = 45678
# backend_socket = None

# def connect_backend():
#     global backend_socket
#     if backend_socket == None:


def Send_pack_to_backend(package_id):
    try:
        print(package_id)
        backend_socket = socket(AF_INET, SOCK_STREAM)
        backend_socket.connect((serverName, packagePort))
        print("Connected to backend.")
        # connect_backend()
        msg = str(package_id) + '\n'
        backend_socket.send(msg.encode('utf-8'))
        return True
    except ConnectionRefusedError:
        return False

def calculate_warehouse(x, y):
    whs = Warehouse.objects.all()
    min = float('inf')
    wh_id = 1
    for wh in whs:
        wh_x = wh.address_x
        wh_y = wh.address_y
        dist = math.sqrt(math.pow(x - wh_x, 2) + math.pow(y - wh_y, 2))
        if min > dist:
            min = dist
            wh_id = wh.id
    return wh_id