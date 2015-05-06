package org.sebersole.paxexam;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class DataPoint {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	private long id;

	private String name;

	public DataPoint() {
	}

	public DataPoint(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
