package com.sjung.sjungbok;


interface AsyncTaskCompleteListener<T> {
	   void onTaskComplete(T result);
	}