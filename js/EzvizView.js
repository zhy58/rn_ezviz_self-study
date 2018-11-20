import {
  requireNativeComponent,
  View,
  NativeModules,
  Platform,
  DeviceEventEmitter
} from 'react-native';

import React, {
  Component,
} from 'react';
import PropTypes from 'prop-types'

export default class Ezviz extends Component {
  static propTypes = {
    ...View.propTypes,
    sound: PropTypes.bool, //声音控制
    currentQulityMode: PropTypes.number,//清晰度控制
    cameraNo: PropTypes.number,//通道控制
    verifyCode: PropTypes.string,//验证码
    accessToken: PropTypes.string,//播放token
    deviceSerial: PropTypes.string,//序列号
  };

  static defaultProps = {
    sound: false,
    currentQulityMode: 2,
    cameraNo: 1,
  };

  constructor() {
    super();
  }

  _onChange(event) {
    if (typeof this.props[event.nativeEvent.type] === 'function') {
      this.props[event.nativeEvent.type](event.nativeEvent.params);
    }
  }

  render() {
    return <EzvizView {...this.props} onChange={this._onChange.bind(this)}/>;
  }
}
const EzvizView = requireNativeComponent('RCTEzvizView', Ezviz, {nativeOnly: {onChange: true}});
