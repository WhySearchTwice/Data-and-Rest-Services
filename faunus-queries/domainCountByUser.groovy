/**
 * Domain Count By User
 *
 * Count the number of times a domain has been visited for each user.
 * Usage: g.V.has('type','user').script('domainCountByUser.groovy')
 *
 * Optional argument 'cleanup' will remove all edges between users and domains
 *
 * @author Tony Grosinger
 */

def g, mode

/**
 * Setup a reference to the graph and save any
 * passed parameters
 */
def setup(args) {
	conf = new org.apache.commons.configuration.BaseConfiguration()
	conf.setProperty('storage.backend', 'hbase')
	conf.setProperty('storage.hostname', 'localhost')
	g = com.thinkaurelius.titan.core.TitanFactory.open(conf)

	if(args != null && args.size() == 1) {
		mode = args[0]
	} else {
		mode = 'upsert'
	}
}

/**
 * Calculate the number of times this user has visited each
 * domain that they have a connection with and save
 */
def map(v, args) {
	// Double check that this vertex is a user
	if(v.type != "user") {
		return
	}

	user = g.v(v.id)
	currentTime = System.currentTimeMillis().toString()

	// Create a map that contains all of the edges from this user to domains
	userDomainPipeline = user.outE('domainVisitCount').as("edge").inV.has('type', 'domain').as("domain").table(new Table()).cap
	domainMap = [:]
	if(userDomainPipeline.hasNext()) {
		results = userDomainPipeline.next()
		for(row in results) {
			domainMap[row[1].id] = row[0]
		}
	}

	if(mode == "cleanup") {
		println("Running cleanup...")

		// Delete all the edges between users and domains
		for(entry in domainMap) {
			entry.value.remove()
		}
	} else {
		// Perform a query, gathering the number of times each domain has been visited by this user
		countPipeline = user.out('owns').out('viewed').out('under').groupCount{it.id}.cap

		if(countPipeline.hasNext()) {
			// Results come in the form of a map like this --> `domainVertexId : count`
			results = countPipeline.next()
			for(entry in results) {
				// Get the domain that we will be linking the user to
				domain = g.v(entry.key)

				// See if there is an existing edge
				if(domainMap[domain.id] != null) {
					edge = domainMap[domain.id]
					if(edge.visits != entry.value) {
						println("Updating")
						edge[currentTime] = entry.value
						edge.visits = entry.value
					}
				} else {
					startingMap = [visits: entry.value, domain: domain.domain]
					startingMap[currentTime] = entry.value
					g.addEdge(user, domain, "domainVisitCount", startingMap)				
				}

				//println("User: " + v.id + ", Domain: " + entry.key + ", Count: " + entry.value + " " + domain.id)
			}
		}
	}
}

/**
 * Perform cleanup steps
 */
def cleanup(args) {
	g.shutdown()
}