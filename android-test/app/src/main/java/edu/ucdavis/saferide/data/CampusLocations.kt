package edu.ucdavis.saferide.data

/**
 * Hardcoded UC Davis places used by the mock service.
 * Coordinates are approximate and only need to look right on the map.
 */
object CampusLocations {

    /** Where the map centers on the home screen. */
    val campusCenter = LatLng(38.5400, -121.7530)

    const val RIDER_NAME = "Alex (UC Davis Student)"

    /** The emulator has no real GPS fix, so "Current Location" is a fixed spot on the Quad. */
    val simulatedCurrentLocation = LatLng(38.54020, -121.75080)

    const val SERVICE_HOURS =
        "Safe Ride service: 6:30pm - 12AM, daily. Off-campus drop offs 9:00pm - 12AM, daily."
    const val VIP_HOURS =
        "VIP Mobility riders can schedule rides 8 hours to 12 weeks in advance, from 6:15am - 11:45pm."

    // Campus locations (valid pickups)
    val shieldsLibrary = campus("shields", "Shields Library", "Shields Library Bus Turnaround", 38.53955, -121.74955)
    val memorialUnion = campus("mu", "Memorial Union", "Memorial Union Bus Terminal", 38.54245, -121.74935)
    val silo = campus("silo", "Silo", "Silo Bus Terminal", 38.53875, -121.75335)
    val coho = campus("coho", "CoHo", "Memorial Union South Entrance (CoHo)", 38.54195, -121.74885)
    val arc = campus("arc", "ARC", "ARC Front Entrance on La Rue Rd", 38.54300, -121.75930)
    val hartHall = campus("hart", "Hart Hall", "Hart Hall on North Quad", 38.54060, -121.75050)
    val mondavi = campus("mondavi", "Mondavi Center", "Mondavi Center Drop-off Loop", 38.53480, -121.74910)

    // Parking lots (on campus, valid pickups)
    val pavilionStructure = parking("pavilion", "Pavilion Parking Structure", "Pavilion Structure Entrance on La Rue Rd", 38.54250, -121.75750)
    val quadStructure = parking("quad_structure", "Quad Parking Structure", "Quad Structure Ground Level", 38.54210, -121.75180)
    val lot5 = parking("lot5", "Lot 5 (Mrak Hall)", "Lot 5 Entrance on Mrak Hall Dr", 38.53780, -121.74700)
    val lot47 = parking("lot47", "Lot 47 (Mondavi Center)", "Lot 47 Pay Station", 38.53400, -121.75050)

    // Off-campus housing (valid drop-offs only)
    val westVillage = housing("west_village", "West Village", "West Village Square", 38.54400, -121.77850)
    val solAtParkside = housing("sol_parkside", "Sol at Parkside", "Sol at Parkside Leasing Office", 38.55250, -121.72950)
    val theUApartments = housing("the_u", "The U Apartments", "The U Apartments Front Gate", 38.55200, -121.76000)

    val campus = listOf(shieldsLibrary, memorialUnion, silo, coho, arc, hartHall, mondavi)
    val parkingLots = listOf(pavilionStructure, quadStructure, lot5, lot47)
    val offCampusHousing = listOf(westVillage, solAtParkside, theUApartments)
    val all = campus + parkingLots + offCampusHousing

    fun inCategory(category: PlaceCategory): List<CampusLocation> = when (category) {
        PlaceCategory.CAMPUS -> campus
        PlaceCategory.PARKING -> parkingLots
        PlaceCategory.HOUSING -> offCampusHousing
    }

    /** Shown under "Recent Places" on first launch, like the current app. */
    val seededRecents = listOf(hartHall, theUApartments)

    /** Rough outline of the main campus. Rides must start inside it. */
    val campusBoundary = listOf(
        LatLng(38.5462, -121.7655),
        LatLng(38.5462, -121.7440),
        LatLng(38.5370, -121.7440),
        LatLng(38.5322, -121.7470),
        LatLng(38.5312, -121.7655),
    )

    /** Rough outline of the SafeRide service area (campus plus the City of Davis), drawn in gold on the map. */
    val serviceArea = listOf(
        LatLng(38.5600, -121.7850),
        LatLng(38.5650, -121.7500),
        LatLng(38.5620, -121.7150),
        LatLng(38.5500, -121.6950),
        LatLng(38.5380, -121.6950),
        LatLng(38.5330, -121.7200),
        LatLng(38.5280, -121.7500),
        LatLng(38.5300, -121.7700),
        LatLng(38.5380, -121.7900),
        LatLng(38.5500, -121.7930),
    )

    private fun campus(id: String, name: String, stop: String, lat: Double, lng: Double) =
        CampusLocation(id, name, stop, LatLng(lat, lng), PlaceCategory.CAMPUS)

    private fun parking(id: String, name: String, stop: String, lat: Double, lng: Double) =
        CampusLocation(id, name, stop, LatLng(lat, lng), PlaceCategory.PARKING)

    private fun housing(id: String, name: String, stop: String, lat: Double, lng: Double) =
        CampusLocation(id, name, stop, LatLng(lat, lng), PlaceCategory.HOUSING)
}
