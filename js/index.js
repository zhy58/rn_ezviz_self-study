import {
  requireNativeComponent,
  View,
  NativeModules,
  Platform,
  DeviceEventEmitter
} from 'react-native';

import React, {
  Component
} from 'react';

import _EzvizView from './EzvizView';
import _EzvizModule from './EzvizModule';
import _EzvizQulityMode from './EzvizQulityMode';

export const EzvizView = _EzvizView;
export const EzvizModule = _EzvizModule;
export const EzvizQulityMode = _EzvizQulityMode;
