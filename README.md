# DailySnipetCodeAndroid

## 1.AdapterRetrofit with RXJava
```kotlin
fun <T> Call<T>.waiting(): Single<Response<T>> = Single.create {single ->

    enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            single.onError(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            single.onSuccess(response)
        }

    })
}
```

