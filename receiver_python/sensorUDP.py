# -*- coding: utf-8 -*-


import numpy as np
import threading
import socket
import sys
import struct
import time
import pickle
import os

from datetime import datetime



current_milli_time = lambda: int(round(time.time() * 1000))
import pandas as pd

class imus_UDP(threading.Thread):
    def __init__(self, Port = 12562, w_size=100*60):
        threading.Thread.__init__(self)

        # keep empty to get all incoming packet
        self.IP = ""

        self.Port = Port
        
        self.PAUSE_loop = True
        self.STOP_loop = False
        
        self.datas = {}
        self.w_size = w_size
        self.INITIALIZED = False
        
        self.saving_folder = "SensorDATA/"
        if not os.path.exists(self.saving_folder):
            os.mkdir(self.saving_folder)
        self.file_prefix = self.saving_folder+datetime.now().strftime('%Y-%m-%d %H_%M_%S')+"_"
        self.online_save_file = {}
        self.save_online = True
        
        
        self.INPUT_EVENT_TYPE = -1
        self.POSE = 0
        
        self.dataFormat = 'ffffqffffffffff' #reverse
        self.data_column_name = "Gx,Gy,Gz,Ax,Ay,Az,ROTx,ROTy,ROTz,ROTw,DeviceTime,DeviceID,MagX,MagY,MagZ" 
        self.numData = len(self.dataFormat)+1
                
        self.delay_ms = {}
        self.refresh_hz = {}
        self.last_times = {}
        self.sync_dt = {}
        
        self.debug_mode = False
        
        self.participant_name = ""
        
        
    def setLogger(self, logger, debug_mode = True):
        self.debug_mode = debug_mode
        self.logger = logger
        
    def rollData(self, data, arr_len, float_arr):
        data[:-1,:] = data[1:,:]
        data[-1,:arr_len+1] = float_arr    
        
#        self.INPUT_EVENT_TYPE = float_arr[-5]
        self.POSE = float_arr[-2]
    def updateDelay(self, addr, n=50):
        local_time, remote_time = self.datas[addr][-n:,0], self.datas[addr][-n:,-1]
        self.delay_ms[addr] = np.average(local_time - remote_time)
        self.last_times[addr] = self.datas[addr][-1,-1]
        
    def updateRefresh(self, addr, n=50):
        remote_time = self.datas[addr][-n:,-1]
        self.refresh_hz[addr] = 1000/np.average(np.diff(remote_time))
        
    def setExchangeQueue(self, q):
        self.queue = q
    def putDataInQueue(self, w_size=300):
        whole_data = {'data': self.getDATA(w_size),
                      'delay_ms': self.delay_ms,
                      'refresh_hz': self.refresh_hz,
                      'last_times': self.last_times,
                      'sync_dt': self.sync_dt}
        self.queue.put(whole_data)
        
    
    # def setLocalIP(self):
    #     self.IP = socket.gethostbyname(socket.gethostname())

    def setIP_Port(self, IP, Port):
        self.IP = IP
        self.Port = Port
        
    def setConnection(self):
        print("CONNECTING...",self.IP,":",self.Port)
        try:
            self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.serverSocket.bind((self.IP, self.Port))
        except:   
            self.serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.serverSocket.bind((self.IP, self.Port))
            
    def resetDATAs(self, new_id=None):
        if new_id != None:
            print("ADD new data incoming: ", new_id)
            self.online_save_file[new_id] = open(self.file_prefix+self.participant_name+"_"+new_id+".csv","w") 
            print("file_created: "+self.file_prefix+self.participant_name+"_"+new_id+".csv")
            if not new_id == "127.0.0.1":
                self.datas[new_id] = np.empty((self.w_size, self.numData))
                self.online_save_file[new_id].write("ServerTime"+self.data_column_name+"\n")

            self.delay_ms[new_id] = 0
            self.refresh_hz[new_id] = 0
            self.last_times[new_id] = 0
            self.sync_dt[new_id] = 0
            
            
            self.INITIALIZED = True
            
        else:
            self.file_prefix = self.saving_folder+datetime.now().strftime('%Y-%m-%d %H_%M_%S')+"_"
        
            for one_key in self.datas.keys():
                self.online_save_file[one_key].close()
                self.online_save_file[one_key] = open(self.file_prefix+self.participant_name+"_"+one_key+".csv","w") 
                if not one_key == "127.0.0.1":
                    self.datas[one_key] = np.empty((self.w_size, self.numData))
                    self.online_save_file[new_id].write("ServerTime"+self.data_column_name+"\n")

                self.delay_ms[new_id] = 0
                self.refresh_hz[new_id] = 0
                self.last_times[new_id] = 0
                self.sync_dt[new_id] = 0
                
                
                                     
            
        print("DATA reset done")    
            
    def getLastData(self, addr=None):
        if not self.INITIALIZED:
            return -1
        if addr==None and len(self.datas)==1:
            return self.datas[list(self.datas.keys())[0]][-1,:]
        else:
            return self.datas[addr][-1,:]
    def getDATA(self, w_size=300):
        if not self.INITIALIZED:
            return -1
        
        if len(self.datas)>0:
            filtered_datas = dict()
            for one_key in self.datas.keys():
                filtered_datas[one_key] = self.datas[one_key][-w_size:,:]
            return filtered_datas
#==============================================================================
#         if len(self.datas)==1:
# #            return self.datas[list(self.datas.keys())[0]][~np.isnan(self.datas[list(self.datas.keys())[0]]).all(axis=1)]
#             return self.datas[list(self.datas.keys())[0]][-w_size,:]
# #            return self.datas[list(self.datas.keys())[0]]
#         else:
#             filtered_datas = dict()
#             for one_key in self.datas.keys():
#                 filtered_datas[one_key] = self.datas[one_key][-w_size,:]
# #                filtered_datas[one_key] = self.datas[one_key][~np.isnan(self.datas[one_key]).all(axis=1)]
#             return filtered_datas
#==============================================================================

           
        
    def run(self):
        self.PAUSE_loop = False
        self.STOP_loop = False
        
        while True:
            
            if self.STOP_loop:
                print("loop STOP")
                break
            
            if not self.PAUSE_loop:            
                try:
                    data, address = self.serverSocket.recvfrom(2048)
                    
                    swap_data = bytearray(data)
                    swap_data.reverse()
                    
                    DATA_NUM = struct.unpack("i", swap_data[-4:])[0]
                    
                    for i in range(DATA_NUM):
                        this_data = swap_data[-(64*(i+1)+4):-(64*i+4)]
                        float_list = struct.unpack(self.dataFormat, this_data)
                        
                        float_arr = np.array(float_list+(current_milli_time(),))
                        float_arr = float_arr[::-1]
                        try:
                            self.rollData(self.datas[address[0]],self.numData,float_arr)
                            self.updateDelay(address[0])
                            self.updateRefresh(address[0])
                            
                            if self.save_online:
                                arr_str = ','.join("{:.2f}".format(x) for x in float_arr)
                                self.online_save_file[address[0]].write(arr_str+"\n")
                        except:
                            if self.debug_mode:
                                self.logger.info("New comer/Error on parsing: {}".format(address))
                            self.resetDATAs(address[0])
                            self.rollData(self.datas[address[0]],self.numData,float_arr)
                    
    
                    
                    
                except socket.error as e:
                    print("Socket Error: %s"%e)
                    if self.STOP_loop:
                        print("loop STOP")
                        break
                
    def __del__(self):
        self.close()
              
    def close(self):
        self.STOP_loop = True
        time.sleep(0.5)
        
        for one_key in self.online_save_file.keys():
            self.online_save_file[one_key].close()
            
        self.Disconnect()
        
        
    
    def Disconnect(self):
        self.serverSocket.close()
        print("imus_UDP Socket closed")
                                    
if __name__ == "__main__":
    imu_get = imus_UDP(Port=12563)
    imu_get.setConnection()
    imu_get.start()
    
    
#    imu_get.startCollecting()
    
    dt = 1
    last_t = 0
    while True:
#        for one in imu_get.datas.keys():
#            print(one, ": ", imu_get.datas[one][-1,:])
        
        try:
            data_chunk = imu_get.getDATA(w_size=500)
        except:
            print("none") 
            
        if type(data_chunk) == dict:
            for key, val in data_chunk.items():
                count = np.count_nonzero(val[:,0]>last_t)
            
                print("from:{} | {}sec count: {}".format(key, dt, count))
            
            one_row = imu_get.getLastData()
            last_t = one_row[0]
        
        time.sleep(dt)
        
    imu_get.stopCollecting()
    data = imu_get.getDATA()
    
    imu_get.close()


