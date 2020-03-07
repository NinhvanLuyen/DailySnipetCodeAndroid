   //Handle add Fragment callback resuult
    private val handlerArray: SparseArray<(resultCode: Int, data: Intent?) -> Unit> = SparseArray()

    protected fun Fragment.startForResult(fragment: Fragment, requestCode: Int,
                                          resultCallBack: (resultCode: Int, data: Intent?) -> Unit):(resultCode:Int,data:Intent?)->Unit {
        showDialogWithRequest(fragment, requestCode)
        handlerArray.append(requestCode, resultCallBack)
        return resultCallBack
    }

    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleOnActivityResult(requestCode, resultCode, data)
    }

    private fun handleOnActivityResult(requestCode: Int,
                                       resultCode: Int, data: Intent?) {
        handlerArray.get(requestCode)?.invoke(resultCode, data)

    }
    protected infix fun Int.isOK(function: ()->Unit){
            if (this == Activity.RESULT_OK)
                function.invoke()
    }
    protected infix fun Int.isCancel(function: ()->Unit){
            if (this == Activity.RESULT_CANCELED)
                function.invoke()
    }
