'use strict';

import {
  requireNativeComponent,
  NativeModules,
  Platform,
  DeviceEventEmitter
} from 'react-native';

import React, {
  Component,
  PropTypes
} from 'react';

const _module = NativeModules.EzvizPlayModule;

export default {
    ezStartConfigureNetwork(deviceSerial, ssid, password) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezStartConfigureNetwork(deviceSerial, ssid, password);
            }
            catch (e) {
                reject(e);
                return;
            }
            // DeviceEventEmitter.once('onConfigureNetworkCallback', resp => {
            //     resolve(resp);
            // });
        });
    },
    ezStopConfigureNetwork() {
        return new Promise((resolve, reject) => {
            try {
                _module.ezStopConfigureNetwork();
            }
            catch (e) {
                reject(e);
                return;
            }
        });
    },
    ezPlay(bool, sound, verifyCode) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezPlay(bool, sound || false, verifyCode || "");
            }
            catch (e) {
                reject(e);
                return;
            }
            DeviceEventEmitter.once('onRealplayCallback', resp => {
                resolve(resp);
            });
        });
    },
    ezSound(bool) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezSound(bool);
            }
            catch (e) {
                reject(e);
                return;
            }
        // DeviceEventEmitter.once('onGetReverseGeoCodeResult', resp => {
        //     resolve(resp);
        // });
        });
    },
    ezTalk(bool, verifyCode) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezTalk(bool, verifyCode ? verifyCode : "");
            }
            catch (e) {
                reject(e);
                return;
            }
            DeviceEventEmitter.once('onRealplayCallback', resp => {
                resolve(resp);
            });
        });
    },
    ezTalking(bool) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezTalking(bool);
            }
            catch (e) {
                reject(e);
                return;
            }
        });
    },
    ezCapture() {
        return new Promise((resolve, reject) => {
            try {
                _module.ezCapture();
            }
            catch (e) {
                reject(e);
                return;
            }
            DeviceEventEmitter.once('onCaptureCallback', resp => {
                resolve(resp);
            });
        });
    },
    ezDefinition(number) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezDefinition(number);
            }
            catch (e) {
                reject(e);
                return;
            }
            DeviceEventEmitter.once('onRealplayCallback', resp => {
                resolve(resp);
            });
        });
    },
    ezFullscreen(bool) {
        return new Promise((resolve, reject) => {
            try {
                _module.ezFullscreen(bool);
            }
            catch (e) {
                reject(e);
                return;
            }
            DeviceEventEmitter.once('orientationDidChange', resp => {
                resolve(resp);
            });
        });
    },
    getOrientation() {
        return _module.getOrientation();
    },
    setOrientation(number){
        return _module.setOrientation(number);
    },
};
