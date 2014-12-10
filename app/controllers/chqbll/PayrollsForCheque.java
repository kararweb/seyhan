/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.chqbll;

import meta.RightBind;
import play.mvc.Controller;
import play.mvc.Result;
import enums.ChqbllSort;

/**
 * @author mdpinar
*/
public class PayrollsForCheque extends Controller {

	public static Result list(RightBind rightBind) {
		return Payrolls.list(ChqbllSort.Cheque, rightBind);
	}

	public static Result save(RightBind rightBind) {
		return Payrolls.save(ChqbllSort.Cheque, rightBind);
	}

	public static Result create(RightBind rightBind) {
		return Payrolls.create(ChqbllSort.Cheque, rightBind);
	}

	public static Result edit(Integer id, RightBind rightBind) {
		return Payrolls.edit(id, ChqbllSort.Cheque, rightBind);
	}

	public static Result remove(Integer id, RightBind rightBind) {
		return Payrolls.remove(id, ChqbllSort.Cheque, rightBind);
	}

}
