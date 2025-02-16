package edu.temple.vehiclecollisiondetection


import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import java.util.Properties

private const val SAVE_KEY = "save_key"
private const val emergencyServiceNum = "+14846391351" //test number (OBV we can't test call 911 whenever we want
class MyBluetoothGattCallback(currentContext: Context, currentActivity: Activity, connectionText: TextView, connectionTipText: TextView) : BluetoothGattCallback(), LocationListener {
    //inherited objects to use with rest of the app
    val activeContext = currentContext
    val activeActivity = currentActivity
    val connectionStatusText = connectionText
    val KEY = BuildConfig.API_KEY
    val connectionTipText = connectionTipText



    //countdown timer object
    private var mCountDownTimer: CountDownTimer? = null
    private val countdownStartTime: Long = 11000 //timer duration for when crashes are detected, current set at 11 seconds (takes a second to popup)
    private var mTimeLeftInMillis = countdownStartTime //variable for tracking countdown duration remaining at a given time
    private var countdownValueInt: Int? = null

    //Location Tracking Stuff
    private lateinit var locationManager: LocationManager
    private var textLat: Double? = 39.981991 //variable used to record Latitude & defaulted to avoid null errors
    private var textLong: Double? = -75.153053 //variable used to record Longitude & defaulted to avoid null errors
    private var textAddress: String = ""

    //object used to get saved data (contact list)
    private lateinit var preferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (ActivityCompat.checkSelfPermission(
                activeContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activeActivity.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 10)
            //return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d("tag1", "Connection Succeeded!")
            activeActivity.runOnUiThread(Runnable() {
                Toast.makeText(activeContext, "Device Connected!", Toast.LENGTH_LONG).show()
                connectionStatusText.setTextColor(Color.parseColor("green"))
                connectionStatusText.setText("Connected!")
                connectionTipText.setVisibility(View.INVISIBLE)
            })
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // handle disconnection
            Log.d("tag2", "Connection Disconnected!")
            activeActivity.runOnUiThread(Runnable() {
                connectionStatusText.setTextColor(Color.parseColor("red"))
                connectionStatusText.setText("Not Connected")
                connectionTipText.setVisibility(View.VISIBLE)
                Toast.makeText(activeContext, "Device Disconnected!", Toast.LENGTH_SHORT).show()
            })
        } else{
            Log.d("tag3", "Connection Attempt Failed!")
            gatt.close()
        }
    }

    @RequiresApi(33)
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

        getLocation() //activates the location tracking when client app is connected to device (to preserve phone battery)
        val serviceUuid = UUID.fromString("00110011-4455-6677-8899-aabbccddeeff")//acts like a 'password' for the bluetooth connection
        val characteristicUuid = UUID.fromString("00112233-4455-6677-8899-abbccddeefff")//acts like a 'password' for the bluetooth connection
        if (ActivityCompat.checkSelfPermission(
                activeContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activeActivity.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 10)
            //return
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val service1 = gatt.getService(serviceUuid)
            val characteristic1 = service1.getCharacteristic(characteristicUuid)//characteristic uuid here
            if(service1 == null){
                Log.d("Invalid service:", "service is null!")
                Log.d("Service UUid:", serviceUuid.toString())
            }else{
                Log.d("Service Found:", serviceUuid.toString())
            }
            if(characteristic1 == null){
                Log.d("Invalid characteristic:", "characteristic is null!")
                Log.d("Characteristic UUid:", characteristicUuid.toString())
            }else{
                Log.d("Characteristic Found:", characteristicUuid.toString())
            }
            gatt.setCharacteristicNotification(characteristic1, true)

            val desc: BluetoothGattDescriptor = characteristic1.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            Log.d("Descriptor Found:", "00002902-0000-1000-8000-00805f9b34fb")
            gatt.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        // handle received data
        Log.d("Characteristic Data", "Data Changed!")
        val data = String(value)

        if(data == "B" || data =="F") {//crash detected!
            getStreetAddress(textLat!!, textLong!!) //default address in case location never updates
            val alertSoundPlayer: MediaPlayer? = MediaPlayer.create(activeContext, R.raw.alert_sound)
            alertSoundPlayer?.start()
            mTimeLeftInMillis = countdownStartTime
            activeActivity.runOnUiThread(){
                //speech recognition initialization
                var speech = SpeechRecognizer.createSpeechRecognizer(activeContext);
                Log.d("VC Status", "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(activeContext));
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                //if a crash is detected by the arduino device, initiate crash popup
                val crashDialogView = LayoutInflater.from(activeContext).inflate(R.layout.crash_procedure_popup, null)
                val crashDialogBuilder = AlertDialog.Builder(activeContext)
                    .setView(crashDialogView)
                    .setTitle("")
                //show dialog
                val crashAlertDialog = crashDialogBuilder.show()
                //countdown
                val countdownTimerText = crashDialogView.findViewById<TextView>(R.id.countdownText)
                mCountDownTimer = object : CountDownTimer(mTimeLeftInMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) { //countdown interval
                        mTimeLeftInMillis = millisUntilFinished
                        countdownValueInt = ((mTimeLeftInMillis / 1000) % 60).toInt()
                        countdownTimerText.setText(countdownValueInt.toString())
                    }
                    override fun onFinish() { //countdown goes to 0
                        mCountDownTimer?.cancel()
                        alertSoundPlayer?.stop()
                        crashAlertDialog.dismiss()
                        speech.stopListening()
                        //get list of saved emergency contacts and text them w/ emergency message
                        preferences = activeActivity.getPreferences(AppCompatActivity.MODE_PRIVATE)
                        val gson = Gson()
                        val serializedList = preferences.getString(SAVE_KEY, null)
                        val myType = object : TypeToken<ArrayList<MainActivity.ContactObject>>() {}.type
                        sendTextsToContacts(gson.fromJson<ArrayList<MainActivity.ContactObject>>(serializedList, myType))
                        //make call to emergency services
                        makeCall(emergencyServiceNum)
                    }
                }.start()
                //Voice Recognition / Control Stuff (setting listener)
                speech!!.setRecognitionListener(object: RecognitionListener{
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {
                        Log.d("VC Stuff", "Beginning of Speech detected!")
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {}
                    override fun onResults(results: Bundle?) {
                        val dataVC = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d("VC RESULT", dataVC!![0])
                        if(dataVC!![0].contains("cancel") || dataVC!![0].contains("stop")){
                            mCountDownTimer?.cancel()
                            speech.stopListening()
                            alertSoundPlayer?.stop()
                            crashAlertDialog.dismiss()
                        } else{//if command not detected, listen again for another command
                            speech.startListening(speechIntent)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}

                })
                //cancel button
                val cancelButton = crashDialogView.findViewById<Button>(R.id.crash_cancel_button)
                cancelButton.setOnClickListener {
                    mCountDownTimer?.cancel()
                    speech.stopListening()
                    alertSoundPlayer?.stop()
                    crashAlertDialog.dismiss()
                }
                speech.startListening(speechIntent)
            }
        }
    }

    var testText = false

    fun sendText(phoneNumber: String, message: String){
        var smsManager: SmsManager? = null
        //var id = SmsManager.getDefaultSmsSubscriptionId()
        smsManager = activeActivity.getSystemService(SmsManager::class.java)

        smsManager?.sendTextMessage(phoneNumber, null, message, null, null)
        Log.d("sendText", "$message sent to $phoneNumber")
        testText = true
    }

    //Test sending to contact list
    var testTexts = 0

    fun sendTextsToContacts(contactObjects: ArrayList<MainActivity.ContactObject>){
        for(obj in contactObjects) {
            //This is for American numbers only!
            val numWithCountryCode = "+1" + obj.phoneNumber

            //Add user variable rather than "someone", add location variable
            sendText(
                numWithCountryCode, "Hello ${obj.name}, I " +
                        "have been in a serious crash. Address: ${textAddress}."
            )
            sendText(
                numWithCountryCode, "Coordinates: Lat-${textLat} Long-${textLong} "
            )

            testTexts += 1
        }
    }

    var testCall = false
    fun makeCall(phoneNumber: String){

        Log.d("Call output", "App is calling $phoneNumber")

        val callIntent = Intent(Intent.ACTION_CALL)
        //start calling intent
        callIntent.data = Uri.parse("tel:$phoneNumber")
        activeContext.startActivity(callIntent)

        testCall = true
    }

    private fun getLocation(){
        activeActivity.runOnUiThread{
            locationManager = activeActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if ((ContextCompat.checkSelfPermission(activeContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(activeActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    @RequiresApi(33)
    override fun onLocationChanged(location: Location) {
        Log.d("Location", "Lat: ${location.latitude}, Long:${location.longitude}")

        textLat = location.latitude
        textLong = location.longitude

        getStreetAddress(textLat!!, textLong!!)
    }


    private fun getStreetAddress(latitude: Double, longitude: Double){
        val queue = Volley.newRequestQueue(activeContext)
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latitude},${longitude}&key=${KEY}"

        Log.d("volley", "Starting volley")
        // Request a string response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val address = response.getJSONArray("results")
                    .getJSONObject(0)
                    .getString("formatted_address")
                Log.d("res", address.toString())

                textAddress = address
            },
            { Log.d("error", "That didn't work") })

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest)
    }
}