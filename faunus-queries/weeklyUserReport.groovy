/**
 * Weekly User Reports
 *
 * For every user, aggregate a set of statistics about their last week
 * of usage. Create a "weeklyReport" vertex tied to the user to store
 * this information.
 * Usage: g.V.has('type','user').script('weeklyUserReport.groovy')
 *
 * Optional argument 'cleanup' will remove all weeklyReport vertices
 *
 * @author Tony Grosinger
 */

def g, mode
def startTime
def endTime

/**
 * Setup a reference to the graph and save any
 * passed parameters
 */
def setup(args) {
	conf = new org.apache.commons.configuration.BaseConfiguration()
	conf.setProperty('storage.backend', 'hbase')
	conf.setProperty('storage.hostname', 'localhost')
	g = com.thinkaurelius.titan.core.TitanFactory.open(conf)

	// Create a starter calendar at the beginning of the day
	Calendar cal = Calendar.getInstance()
	cal.set(Calendar.HOUR, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    // Find the first day of last week
    cal.add(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK) - 7)
    startTime = cal.getTimeInMillis()

    // and add six days to get the end date
    cal.add(Calendar.DAY_OF_YEAR, 7)
    endTime = cal.getTimeInMillis()

    println("Start: " + startTime + " - End: " + endTime)

	if(args != null && args.size() == 1) {
		mode = args[0]
	} else {
		mode = 'generate'
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

	if(mode == "cleanup") {
		println("Running cleanup...")

		// Find all weeklyReports and the edge that connects them
		weeklyReportPipeline = user.outE('weeklyReport').as("edge").inV.has('type', 'weeklyReport').as("report").table(new Table()).cap
		if(weeklyReportPipeline.hasNext()) {
			results = weeklyReportPipeline.next()
			for(row in results) {
				// Delete the edge & vertex
				row[0].remove()
				row[1].remove()
			}
		}
	} else {
		println("Running report for user " + v.id + "...")

		// Top ten domains by visit count
		//topDomainsByCountPipeline = user.out('owns').out('viewed').filter{it.pageOpenTime > startTime && it.pageCloseTime != null && it.pageCloseTime < endTime}.out('under').groupCount{it.domain}.cap
		//topTenDomainsByCount = topDomainsByCountPipeline.next().sort{a, b -> b.value <=> a.value}[0..9]
		
		// Top ten domains by time spent
		domainStats = [:]
		deviceStats = [:]

		// Create an iterator over a set of lists in the form [deviceGuid, domainGuid, time on page]
		topDomainsByTimePipeline = user.out('owns').id.as('device').back(1).out('viewed').filter{it.pageOpenTime > startTime && it.pageCloseTime != null && it.pageCloseTime < endTime}.as('pv').out('under').id.as('domain').transform{p,m -> [m.device, m.domain, m.pv.pageCloseTime - m.pv.pageOpenTime]}
		while(topDomainsByTimePipeline.hasNext()) {
			row = topDomainsByTimePipeline.next()
			deviceGuid = row[0]
			domainGuid = row[1]
			timeVisited = row[2]

			// Set device stats
			if(deviceStats[deviceGuid] != null) {
				deviceStats[deviceGuid][1] += 1
				deviceStats[deviceGuid][2] += timeVisited
			} else {
				deviceStats[deviceGuid] = [deviceGuid, 1, timeVisited]
			}

			// Set domain stats
			if(domainStats[domainGuid] != null) {
				domainStats[domainGuid][1] += 1
				domainStats[domainGuid][2] += timeVisited
			} else {
				domainStats[domainGuid] = [domainGuid, 1, timeVisited]
			}
		}

		// Sort the result maps
		domainStatsByCount = domainStats.values().sort{ a, b -> b[1] <=> a[1] }
		domainStatsByTime = domainStats.values().sort{ a, b -> b[2] <=> a[2] }
		deviceStatsByCount = deviceStats.values().sort{ a, b -> b[1] <=> a[1] }
		deviceStatsByTime = deviceStats.values().sort{ a, b -> b[2] <=> a[2] }
	}
}

/**
 * Perform cleanup steps
 */
def cleanup(args) {
	g.shutdown()
}