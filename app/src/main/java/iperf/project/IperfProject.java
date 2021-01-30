package iperf.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import iperf.project.R;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import static android.content.ContentValues.TAG;
import static android.provider.Settings.System.*;

//Main class of the activity
public class IperfProject extends Activity {
	
	String txtFileName = "iperf.txt";

	public IperfProject() {
	}

	//A global pointer for instances of iperf (only one at a time is allowed).
	IperfTask iperfTask = null;
	
	//This is a required method that implements the activity startup
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Shows the logo screen, and waits for a tap to continue to the main screen
		setContentView(R.layout.iperf_activity);
		//Get Wifi connection information and displays it in the main screen
		// getCurrentIP();
		getLocalIpAddress("IPv4");
	}

	//Used to retrieve WiFi connection information and check if a connection exists. If not, display an error on the main screen, otherwise displays the local IP.
	public void getCurrentIP() {
		final TextView ip = (TextView) findViewById(R.id.ip);
		//An instance of WifiManger is used to retrieve connection info.
		WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);
		if (wim.getConnectionInfo() != null) {
			if ((wim.getConnectionInfo().getIpAddress()) != 0) {
				//IP is parsed into readable format
				ip.append("Your IP address is: "
						+ Formatter.formatIpAddress(wim.getConnectionInfo()
								.getIpAddress()));
			} else {
				ip.append("Error: a wifi connection cannot be detected.");
			}
		} else {
			ip.append("Error: a wifi connection cannot be detected.");
		}
	}
    // New method for getting IP_Address of connected device.
	public void getLocalIpAddress(String inettype) {
		final TextView ip = (TextView) findViewById(R.id.ip);
		ip.setText(null);
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); ((Enumeration) en).hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (inettype == "IPv6") {
						if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
							Pattern pattern = Pattern.compile("wlan", Pattern.CASE_INSENSITIVE);
							Matcher matcher = pattern.matcher(intf.getDisplayName());
							boolean matchFound = matcher.find();
							if (matchFound) {
								ip.append("Your IP address is: " + inetAddress.getHostAddress());
								return;
							} else {
								ip.append("Error: a wifi connection cannot be detected.");
								return;
							}
						}
					} else if (inettype == "IPv4") {
						if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
							Pattern pattern = Pattern.compile("wlan", Pattern.CASE_INSENSITIVE);
							Matcher matcher = pattern.matcher(intf.getDisplayName());
							boolean matchFound = matcher.find();
							if (matchFound) {
								ip.append("Your IP address is: " + inetAddress.getHostAddress());
								return;
							} else {
								ip.append("Error: a wifi connection cannot be detected.");
								return;
							}
						}
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
	}

	//This function is used to copy the iperf executable to a directory which execute permissions for this application, and then gives it execute permissions.
	//It runs on every initiation of an iperf test, but copies the file only if it's needed.
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public void initIperf() {
		final TextView tv = (TextView) findViewById(R.id.OutputText);
		InputStream in;
		try {
			//The asset "iperf" (from assets folder) inside the activity is opened for reading.
			in = getResources().getAssets().open("iperf2014a");
		} catch (IOException e2) {
			tv.append("\nError occurred while accessing system resources, please reboot and try again.");
			return;			
		}
		try {
			//Checks if the file already exists, if not copies it.
			new FileInputStream("/data/data/iperf.project/iperf2014a");
		} catch (FileNotFoundException e1) {
			try {
				//The file named "iperf" is created in a system designated folder for this application.
				OutputStream out = new FileOutputStream("/data/data/iperf.project/iperf2014a", false);
				// Transfer bytes from "in" to "out"
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				//After the copy operation is finished, we give execute permissions to the "iperf" executable using shell commands.
				Process processChmod = Runtime.getRuntime().exec("/system/bin/chmod 744 /data/data/iperf.project/iperf2014a");
				// String iperflibpath = getApplicationInfo().nativeLibraryDir + "/iperf2014a";
				// Process processChmod = Runtime.getRuntime().exec("/system/bin/chmod 744 /data/data/iperf.project/iperf2014a");
				// Executes the command and waits untill it finishes.
				processChmod.waitFor();
			} catch (IOException e) {
				tv.append("\nError occurred while accessing system resources, please reboot and try again.");
				return;
			} catch (InterruptedException e) {
				tv.append("\nError occurred while accessing system resources, please reboot and try again.");
				return;
			}		
			//Creates an instance of the class IperfTask for running an iperf test, then executes.
			iperfTask = new IperfTask();
			iperfTask.execute();				
			return;					
		}
		try {
			Runtime.getRuntime().exec("chmod -R 774 " + "/data/data/iperf.project/iperf2014a");
		} catch (IOException e) {
			tv.append("\nProcess Failed");
		}
		//Creates an instance of the class IperfTask for running an iperf test, then executes.
		iperfTask = new IperfTask();
		iperfTask.execute();
		return;
	}

	//This method is used to handle start-abort toggle button clicks
	public void ToggleButtonClick(View v) {
		final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		//If the button is not pushed (waiting for starting a test), then a iperf task is started.
		if (toggleButton.isChecked()) {
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(inputCommands.getWindowToken(), 0);
			initIperf();
		//If a test is already running then a cancel command is issued through the iperfTask interface.
		} else {
			if (iperfTask == null){
				toggleButton.setChecked(false);
				return;
			}
			iperfTask.onCancelled();
			// iperfTask.cancel(true);
			iperfTask = null;
		}
	}

	// This method is used to handle IPv4-IPv6 toggle button clicks
	public void ipvToggleButtonClick(View v) {
		final ToggleButton iptoggleButton = (ToggleButton) findViewById(R.id.ipvButton);
		if (iptoggleButton.isChecked()) {
			getLocalIpAddress("IPv6");
		} else {
			getLocalIpAddress("IPv4");
		}
	}
	//This method is used to handle the save button click
	public void SaveButtonClick(View v) {
		final TextView tv = (TextView) findViewById(R.id.OutputText);
		
		//Create a dialog for filename input
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		input.setHint("Please enter a filename");
		alert.setView(input);
		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				File sdroot = Environment.getExternalStorageDirectory();
				// Check for permissions if not get it from user.
				if (!sdroot.canWrite()) {
					requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.READ_EXTERNAL_STORAGE}, 2909);
					tv.append("\n Try now to save log file..");
				}
				try {
					if (sdroot.canWrite()) {
						//Save file on SD card
						File txtfile = new File(sdroot, (value + ".txt"));
						FileWriter txtwriter = new FileWriter(txtfile);
						BufferedWriter out = new BufferedWriter(txtwriter);
						out.write(tv.getText().toString());
						out.close();
						tv.append("\nLog file saved.");
					}
				} catch (IOException e) {
					tv.append("\nError occurred while saving log file.");
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();
		

	}
	
	
	//This is used to switch from the logo screen to the main screen with animation.
	public void SkipWelcome(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(),R.anim.fade));
		switcher.setFlipInterval(2000);
		switcher.startFlipping();
		switcher.showNext();
		switcher.stopFlipping();
	}
	//This is used to switch from the main screen to the help screen with animation.
	public void GoToHelp(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(inputCommands.getWindowToken(), 0);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(),R.anim.fade));
		switcher.showNext();

	}
	
	//This is used to switch from the help screen to the main screen with animation.
	public void ReturnFromHelp(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(),R.anim.fade));
		switcher.showPrevious();
	}
	
	//This is used to display copyright information on the logo screen when the "i" is taped.
	public void ShowCopy(View v) {
		final TextView i = (TextView) findViewById(R.id.aboutInfo);	
		i.setTextSize(13);
		i.setText(R.string.copyright);
	}

	//The main class for executing iperf instances.
	//With every test started, an instance of this class is created, and is destroyed when the test is done.
	//This class extends the class AsyncTask which is used to perform long background tasks and allow updates to the GUI while running.
	//This is done by overriding certain functions that offer this functionality.
	class IperfTask extends AsyncTask<Void, String, String> {
		final TextView tv = (TextView) findViewById(R.id.OutputText);
		final ScrollView scroller = (ScrollView) findViewById(R.id.Scroller);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

		Process process = null;

		//This function is used to implement the main task that runs on the background.
		@Override
		protected String doInBackground(Void... voids) {
			//Iperf command syntax check using a Regular expression to protect the system from user exploitation.
			String str = inputCommands.getText().toString();
			if (!str.matches("(iperf )?((-[s,-server])|(-[c,-client] ([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5]))|(-[c,-client] \\w{1,63})|(-[h,-help]))(( -[f,-format] [bBkKmMgG])|(\\s)|( -[l,-len] \\d{1,5}[KM])|( -[B,-bind] \\w{1,63})|( -[r,-tradeoff])|( -[v,-version])|( -[V,-V6])|( -[N,-nodelay])|( -[T,-ttl] \\d{1,8})|( -[U,-single_udp])|( -[d,-dualtest])|( -[w,-window] \\d{1,5}[KM])|( -[n,-num] \\d{1,10}[KM])|( -[p,-port] \\d{1,5})|( -[L,-listenport] \\d{1,5})|( -[t,-time] \\d{1,8})|( -[i,-interval] \\d{1,4})|( -[u,-udp])|( -[b,-bandwidth] \\d{1,20}[bBkKmMgG])|( -[m,-print_mss])|( -[P,-parallel] d{1,2})|( -[M,-mss] d{1,20}))*")) {
				if (!str.matches("(iperf )?((-[s,-server])|(-[c,-client] (.*)::(.*):(.*):(.*):(.*))|(-[c,-client] \\w{1,63})|(-[h,-help]))(( -[f,-format] [bBkKmMgG])|(\\s)|( -[l,-len] \\d{1,5}[KM])|( -[B,-bind] \\w{1,63})|( -[r,-tradeoff])|( -[v,-version])|( -[V,-V6])|( -[N,-nodelay])|( -[T,-ttl] \\d{1,8})|( -[U,-single_udp])|( -[d,-dualtest])|( -[w,-window] \\d{1,5}[KM])|( -[n,-num] \\d{1,10}[KM])|( -[p,-port] \\d{1,5})|( -[L,-listenport] \\d{1,5})|( -[t,-time] \\d{1,8})|( -[i,-interval] \\d{1,4})|( -[u,-udp])|( -[b,-bandwidth] \\d{1,20}[bBkKmMgG])|( -[m,-print_mss])|( -[P,-parallel] d{1,2})|( -[M,-mss] d{1,20}))*")) {
					publishProgress("Error: invalid syntax. Please try again.\n\n");
					return null;
				}
			}
			try {
				//The user input for the parameters is parsed into a string list as required from the ProcessBuilder Class.
				String[] commands = inputCommands.getText().toString().split(" ");
				List<String> commandList = new ArrayList<String>(Arrays.asList(commands));
				//If the first parameter is "iperf", it is removed
				if (commandList.get(0).equals((String)"iperf")) {
					commandList.remove(0);
				}
				//The execution command is added first in the list for the shell interface.
				commandList.add(0,"/data/data/iperf.project/iperf2014a");
				//The process is now being run with the verified parameters.
				process = new ProcessBuilder().command(commandList).redirectErrorStream(true).start();
				//A buffered output of the stdout is being initialized so the iperf output could be displayed on the screen.
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				int read;
				//The output text is accumulated into a string buffer and published to the GUI
				char[] buffer = new char[4096];
				StringBuffer output = new StringBuffer();
				while ((read = reader.read(buffer)) > 0) {
					output.append(buffer, 0, read);
					//This is used to pass the output to the thread running the GUI, since this is separate thread.
					publishProgress(output.toString());
					output.delete(0, output.length());
				}
				reader.close();
				process.destroy();
			}
			catch (IOException e) {
				// publishProgress("\nError occurred while accessing system resources, please reboot and try again.");
				e.printStackTrace();
			}
			return null;
		}

		//This function is called by AsyncTask when publishProgress is called.
		//This function runs on the main GUI thread so it can publish changes to it, while not getting in the way of the main task.
		@Override
		public void onProgressUpdate(String... strings) {
			tv.append(strings[0]);
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});
		}

		//This function is called by the AsyncTask class when IperfTask.cancel is called.
		//It is used to terminate an already running task.
		@Override
		public void onCancelled() {
			//The running process is destroyed and system resources are freed.
			if (process != null) {
				process.destroy();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
			//The toggle button is switched to "off"
			toggleButton.setChecked(false);
			tv.append("\nOperation aborted.\n\n");
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});
		}

		@Override
		public void onPostExecute(String result) {
			//The running process is destroyed and system resources are freed.
			if (process != null) {
				process.destroy();
			
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				tv.append("\n");
			}
			//The toggle button is switched to "off"
			toggleButton.setChecked(false);
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});
		}
	}

}
