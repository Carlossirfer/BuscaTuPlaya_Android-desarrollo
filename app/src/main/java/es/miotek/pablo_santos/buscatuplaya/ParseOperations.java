package es.miotek.pablo_santos.buscatuplaya;

import android.content.Context;

import com.parse.Parse;

/**
 * Created by Pablo_Santos on 12/06/2017.
 * <p>
 * Clase que contendrá todas las operaciones de Parse que realizará la aplicación. De momento
 * está sin uso.
 */

public class ParseOperations {

	private Context context;

	// Recoge el contexto e inicializa Parse.
	public ParseOperations(Context context) {
		this.context = context;
		Parse.initialize(context);
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public static void getAll() {

	}

}
