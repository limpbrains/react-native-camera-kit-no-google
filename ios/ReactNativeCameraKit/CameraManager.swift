//
//  CameraManager.swift
//  ReactNativeCameraKit
//

import AVFoundation
import CoreImage
import Foundation
import React
import Vision

/*
 * Class managing the communication between React Native and the native implementation
 */
@objc(CKCameraManager) public class CameraManager: RCTViewManager {
    override public static func requiresMainQueueSetup() -> Bool {
        return true
    }

    override public func view() -> UIView! {
        return CameraView()
    }

    @objc public static func capture(camera: CameraView,
                       options: NSDictionary,
                       resolve: @escaping RCTPromiseResolveBlock,
                       reject: @escaping RCTPromiseRejectBlock) {
        camera.capture(onSuccess: { resolve($0) },
                    onError: { reject("capture_error", $0, nil) })
    }

    @objc public static func checkDeviceCameraAuthorizationStatus(_ resolve: @escaping RCTPromiseResolveBlock,
                                                    reject: @escaping RCTPromiseRejectBlock) {
        #if targetEnvironment(macCatalyst)
        if #available(macCatalyst 14.0, *) {
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: resolve(true)
            case .notDetermined: resolve(-1)
            default: resolve(false)
            }
        } else {
            resolve(false)
        }
        #else
        switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: resolve(true)
            case .notDetermined: resolve(-1)
            default: resolve(false)
        }
        #endif
    }

    @objc public static func requestDeviceCameraAuthorization(_ resolve: @escaping RCTPromiseResolveBlock,
                                                reject: @escaping RCTPromiseRejectBlock) {
        #if targetEnvironment(macCatalyst)
        if #available(macCatalyst 14.0, *) {
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { resolve($0) })
        } else {
            resolve(false)
        }
        #else
        AVCaptureDevice.requestAccess(for: .video, completionHandler: { resolve($0) })
        #endif
    }

    @objc public static func detectQRCodeInImage(_ base64: String,
                                                  resolve: @escaping RCTPromiseResolveBlock,
                                                  reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.global(qos: .userInitiated).async {
            guard let data = Data(base64Encoded: base64, options: .ignoreUnknownCharacters) else {
                reject("E_INVALID_IMAGE", "Could not decode base64 image data", nil)
                return
            }

            #if targetEnvironment(simulator)
            guard let ciImage = CIImage(data: data) else {
                reject("E_INVALID_IMAGE", "Could not decode base64 image data", nil)
                return
            }
            guard let detector = CIDetector(ofType: CIDetectorTypeQRCode,
                                            context: nil,
                                            options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]) else {
                reject("E_QR_DETECTION_FAILED", "Could not initialize QR detector", nil)
                return
            }
            let features = detector.features(in: ciImage) as? [CIQRCodeFeature]
            let value = features?.first?.messageString
            resolve(value?.isEmpty == false ? value : nil)
            #else
            guard let uiImage = UIImage(data: data),
                  let cgImage = uiImage.cgImage else {
                reject("E_INVALID_IMAGE", "Could not decode base64 image data", nil)
                return
            }
            let request = VNDetectBarcodesRequest()
            request.symbologies = [.qr]
            let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
            do {
                try handler.perform([request])
            } catch {
                reject("E_QR_DETECTION_FAILED", "Vision request failed: \(error.localizedDescription)", error)
                return
            }
            let value = request.results?.first?.payloadStringValue
            resolve(value?.isEmpty == false ? value : nil)
            #endif
        }
    }
}
