jest.mock('react-native', () => ({
  NativeModules: { CameraKit: {} },
  Platform: { OS: 'ios' },
  TurboModuleRegistry: { getEnforcing: jest.fn(() => ({})) },
}));

jest.mock('../specs/NativeCameraKitModule', () => ({
  __esModule: true,
  default: {
    detectQRCodeInImage: jest.fn(),
  },
}));

import NativeCameraKitModule from '../specs/NativeCameraKitModule';
import { detectQRCodeInImage } from '../index';

const mockedDetect = NativeCameraKitModule.detectQRCodeInImage as jest.Mock;

describe('detectQRCodeInImage', () => {
  beforeEach(() => {
    mockedDetect.mockReset();
  });

  it('forwards the base64 argument to the native module', async () => {
    mockedDetect.mockResolvedValueOnce('decoded-value');

    await detectQRCodeInImage('base64-data');

    expect(mockedDetect).toHaveBeenCalledTimes(1);
    expect(mockedDetect).toHaveBeenCalledWith('base64-data');
  });

  it('resolves with the decoded string from the native module', async () => {
    mockedDetect.mockResolvedValueOnce('https://example.com');

    await expect(detectQRCodeInImage('xyz')).resolves.toBe('https://example.com');
  });

  it('resolves with null when no QR code was found', async () => {
    mockedDetect.mockResolvedValueOnce(null);

    await expect(detectQRCodeInImage('xyz')).resolves.toBeNull();
  });

  it('propagates native rejections (e.g. invalid image)', async () => {
    const err = new Error('Could not decode base64 image data');
    mockedDetect.mockRejectedValueOnce(err);

    await expect(detectQRCodeInImage('xyz')).rejects.toBe(err);
  });
});
