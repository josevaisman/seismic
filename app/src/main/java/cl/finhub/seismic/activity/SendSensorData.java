
package cl.finhub.seismic.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SendSensorData extends BroadcastReceiver {    
    @Override
    public void onReceive(Context context, Intent intent) {
		
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {    
			
			Intent myStarterIntent = new Intent(context, MainActivity.class);
			myStarterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(myStarterIntent);

		}    
    }   
}
