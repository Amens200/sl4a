/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.ase.facade;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;

import com.google.ase.AseLog;
import com.google.ase.jsonrpc.RpcReceiver;
import com.google.ase.rpc.Rpc;
import com.google.ase.rpc.RpcParameter;

public class CameraFacade implements RpcReceiver {

  private class BooleanResult {
    boolean mmResult;
  }

  private Camera mCamera;

  @Rpc(description = "Open the camera for use.")
  public void cameraOpen() {
    mCamera = Camera.open();
  }

  @Rpc(description = "Close the camera so that other processes may use it.")
  public void cameraClose() {
    if (mCamera != null) {
      mCamera.release();
    }
    mCamera = null;
  }

  @Rpc(description = "Auto focuses the camera.")
  public Boolean cameraAutoFocus() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final BooleanResult result = new BooleanResult();
    mCamera.autoFocus(new AutoFocusCallback() {
      @Override
      public void onAutoFocus(boolean success, Camera camera) {
        result.mmResult = success;
        latch.countDown();
      }
    });
    latch.await();
    return result.mmResult;
  }

  @Rpc(description = "Take a picture and save it to the specified path.", returns = "True on success.")
  public Boolean cameraTakePicture(@RpcParameter(name = "path") final String path)
      throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final BooleanResult result = new BooleanResult();
    mCamera.takePicture(null, null, new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        try {
          FileOutputStream output = new FileOutputStream(path);
          output.write(data);
          output.close();
          result.mmResult = true;
        } catch (FileNotFoundException e) {
          AseLog.e("Failed to save picture.", e);
          result.mmResult = false;
          return;
        } catch (IOException e) {
          AseLog.e("Failed to save picture.", e);
          result.mmResult = false;
          return;
        } finally {
          latch.countDown();
        }
      }
    });
    latch.await();
    return result.mmResult;
  }

  @Override
  public void shutdown() {
    cameraClose();
  }
}