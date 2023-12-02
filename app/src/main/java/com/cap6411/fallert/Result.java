package com.cap6411.fallert;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class Result {
        public Bitmap src_img = null;
        public List<Float[]> det_bboxes = new ArrayList<>();
        public List<Float> det_scores = new ArrayList<>();
        public List<Float> det_labels = new ArrayList<>();
        public List<List<Float[]>> det_kpts_xyconf = new ArrayList<>();
        public long processTimeMs;

        public Result() {}
    }