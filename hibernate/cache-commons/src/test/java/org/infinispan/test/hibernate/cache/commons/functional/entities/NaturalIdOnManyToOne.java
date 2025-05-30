package org.infinispan.test.hibernate.cache.commons.functional.entities;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
@NaturalIdCache
/**
 * Test case for NaturalId annotation - ANN-750
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class NaturalIdOnManyToOne {
	@Id
	@GeneratedValue
	int id;

	@Transient
	long version;

	@NaturalId
	@ManyToOne
	Citizen citizen;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Citizen getCitizen() {
		return citizen;
	}

	public void setCitizen(Citizen citizen) {
		this.citizen = citizen;
	}
}
