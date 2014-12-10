/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import enums.TransStatus;

@Entity
/**
 * @author mdpinar
*/
public class WaybillTransDetail extends AbstractStockTransDetail {

	private static final long serialVersionUID = 1L;

	public TransStatus status = TransStatus.Waiting;

	@ManyToOne
	public WaybillTrans trans;

	@ManyToOne
	public WaybillTransSource transSource;

	public Double completed = 0d;
	public Double cancelled = 0d;

	/*
	 * devir fisleri icin true degeri alir
	 */
	public Boolean isTransfer = Boolean.FALSE;

}
