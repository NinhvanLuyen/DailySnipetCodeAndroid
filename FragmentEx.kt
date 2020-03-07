   //Handle add Fragment callback resuult

    //We using array to save list callback with key:requestCode and value: is function callback 
    private val handlerArray: SparseArray<(resultCode: Int, data: Intent?) -> Unit> = SparseArray()

    //Everytime we start a fragment we need add function callback to handlerArray
    protected fun Fragment.startForResult(areaShowFragment:Int ,fragment: Fragment, requestCode: Int,
                                          resultCallBack: (resultCode: Int, data: Intent?) -> Unit):(resultCode:Int,data:Intent?)->Unit {
        addFragmentForResult(fragment, requestCode,areaShowFragment)
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
