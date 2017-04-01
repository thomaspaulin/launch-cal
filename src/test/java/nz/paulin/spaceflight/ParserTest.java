package nz.paulin.spaceflight;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;


public class ParserTest extends Assert {
    private static Document document;

    @BeforeClass
    public static void crawl() {
//        try {
//            String html = Files.lines(new File(ParserTest.class.getResource("schedule-2017-03-31.html").toURI()).toPath()).reduce(String::concat).toString();
//            document = Jsoup.parse(html);
//        } catch (IOException | URISyntaxException e) {
//            e.printStackTrace();
//            fail("Set up failed");
//        }
    }

    @Test
    public void missionParsing_MonthButNoDay_EmptyResultList() throws ParseException {
        String html = "<table class=\"launchcalendar netm\" id=\"launch-276\"><tbody><tr><th><span class=\"net\">NET</span><span>Sep</span></th><th colspan=\"2\">STP-2</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/usaf.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/falcon_heavy.png');\"></div><br>Falcon Heavy</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/spacex.png');\"><table><tbody><tr><th>Location</th><td>Kennedy Space Center LC-39A</td></tr><tr><th>Time</th><td>TBD</td></tr></tbody></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>The Space Test Program-2 (STP-2) mission is comprised of a cluster of military and scientific research satellites for the United States Air Force (USAF).</p><div></div></td></tr></tbody></table>";
        assertTrue(Parser.parseLaunches(html).isEmpty());
    }

    @Test
    public void missionParsing_TimeTBDNoWindow_EmptyResultList() throws ParseException {
        String html = "<table class=\"launchcalendar netd\" id=\"launch-250\"><tr><th><span class=\"net\">NET</span><span>Apr 4</span></th><th colspan=\"2\">GSAT-9</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/isro.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/gslv_ii.png');\"></div><br />GSLV Mk II</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/isro.png');\"><table><tr><th>Location</th><td>Satish Dhawan Space Centre SLP</td></tr><tr><th>Time</th><td>TBD</td></tr></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>Launching on the GSLV-F09 mission, GSAT-9 is a multi-band communications satellite for India. The satellite will provide global positioning services for navigation, surveillance/Air Traffic Management systems over Indian airspace via the GAGAN system (GPS Aided GEO Augmented Navigation). </p><div></div></td></tr></table>";
        assertTrue(Parser.parseLaunches(html).isEmpty());
    }

    @Test
    @Ignore("Haven't encountered this case yet. Not sure if it's even possible")
    public void missionParsing_TimeWithNoWindow_Parses() {
    }

    @Test
    public void missionParsing_AllPresent_Parses() throws ParseException {
        String html = "<table class=\"launchcalendar set\" id=\"launch-251\"><tbody><tr><th><span class=\"net\"></span><span>Apr 4</span></th><th colspan=\"2\">SES-15</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/misc.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/soyuz-2.png');\"></div><br>Soyuz-<wbr>2 (Arianespace)</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/arianespace.png');\"><table><tbody><tr><th>Location</th><td>Guiana Space Centre ELS</td></tr><tr><th>Time</th><td>11:47:52 AM GFT (UTC-3)</td></tr><tr><th>Window</th><td> 1 second</td></tr></tbody></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>An Arianespace Soyuz rocket, designated VS17, will launch the SES-15 communications satellite into geostationary orbit. Built by Boeing and equipped with an all-electric propulsion system, SES 15 is a high throughput satellite operating in the Ku- and Ka-bands supporting government, networking, airline, and maritime customers across North America.</p><div></div></td></tr></tbody></table>";
        Launch launch = Parser.parseLaunches(html).get(0);
        testParsedLaunch("SES-15", "Guiana Space Centre ELS", ZonedDateTime.of(2017, 4, 4, 11, 47, 52, 0, ZoneId.of("America/Cayenne")), 1, "Soyuz-2 (Arianespace)", "An Arianespace Soyuz rocket, designated VS17, will launch the SES-15 communications satellite into geostationary orbit. Built by Boeing and equipped with an all-electric propulsion system, SES 15 is a high throughput satellite operating in the Ku- and Ka-bands supporting government, networking, airline, and maritime customers across North America.", launch);

        html = "<table class=\"launchcalendar set\" id=\"launch-243\"><tbody><tr><th><span class=\"net\"></span><span>Apr 20</span></th><th colspan=\"2\">Soyuz MS-04</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/iss.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/soyuz-fg.png');\"></div><br>Soyuz-<wbr>FG</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/tsskb-progress.png');\"><table><tbody><tr><th>Location</th><td>Baikonur Cosmodrome LC-1</td></tr><tr><th>Time</th><td>1:13 PM ALMT (UTC+6)</td></tr><tr><th>Window</th><td> 1 second</td></tr></tbody></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>The Soyuz MS-04 spacecraft will transport two members of the Expedition 51 crew to the International Space Station, NASA astronaut Jack D. Fischer and Roscosmos cosmonaut Fyodor Yurchikhin. MS-04 will be the 133rd flight of a Soyuz spacecraft.</p><div></div></td></tr></tbody></table>";
        launch = Parser.parseLaunches(html).get(0);
        testParsedLaunch("Soyuz MS-04", "Baikonur Cosmodrome LC-1", ZonedDateTime.of(2017, 4, 20, 13, 13, 0, 0, ZoneId.of("Asia/Almaty")), 1, "Soyuz-FG", "The Soyuz MS-04 spacecraft will transport two members of the Expedition 51 crew to the International Space Station, NASA astronaut Jack D. Fischer and Roscosmos cosmonaut Fyodor Yurchikhin. MS-04 will be the 133rd flight of a Soyuz spacecraft.", launch);
    }

    @Test
    public void missionParsing_TimeTBDWindowPresent_EmptyResultList() throws ParseException {
        String html = "<table class=\"launchcalendar netm\" id=\"launch-245\"><tbody><tr><th><span class=\"net\">NET</span><span>Mar</span></th><th colspan=\"2\">SGDC-1 and Koreasat-7</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/misc.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/ariane_5_eca.png');\"></div><br>Ariane 5 ECA</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/arianespace.png');\"><table><tbody><tr><th>Location</th><td>Guiana Space Centre ELA-3</td></tr><tr><th>Time</th><td>TBD</td></tr><tr><th>Window</th><td>2  hours, 49 minutes</td></tr></tbody></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>Built on the Upgraded Spacebus-4000B2 platform from Thales Alenia Space, Koreasat-7 is a Ku-band communications satellite for KTsat based in South Korea. Koreasat-7's coverage zone encompasses Korea, the Philippines, Indonesia, and India. Brazilian state-owned telecommunications services provider Telebras SGDC-1 (Geostationary Defense and Strategic Communications Satellite) is a X- and Ka-band satellite for military and civil use.</p><div><h4>Related Articles:</h4><ul><li><a href=\"http://www.spaceflightinsider.com/organizations/arianespace/social-movement-continues-delay-ariane-5-launch/\"><span class=\"date\">Mar 2017: </span>‘Social movement’ continues to delay Ariane 5 launch</a></li><li><a href=\"http://www.spaceflightinsider.com/organizations/arianespace/arianespace-delays-launch-ariane-5-flight-va236-due-social-movement/\"><span class=\"date\">Mar 2017: </span>Arianespace delays launch of Ariane 5 Flight VA236 due to ‘social movement’</a></li><li><a href=\"http://www.spaceflightinsider.com/organizations/arianespace/arianespace-set-launch-two-telecom-satellites-second-ariane-5-mission-2017/\"><span class=\"date\">Mar 2017: </span>Arianespace set to launch two telecom satellites in second Ariane 5 mission of 2017</a></li></ul></div></td></tr></tbody></table>";
        assertTrue(Parser.parseLaunches(html).isEmpty());
    }

    @Test
    public void missionParsing_NETMonthAndDayTimeTBD_EmptyResultList() throws ParseException {
        String html = "<table class=\"launchcalendar netd\" id=\"launch-259\"><tbody><tr><th><span class=\"net\">NET</span><span>Aug 31</span></th><th colspan=\"2\">NROL-52</th></tr><tr><td rowspan=\"2\" class=\"vehicle\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/operators_large/nro.png');\"><div style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/vehicles/atlas_v_4x0.png');\"></div><br>Atlas V 421</td><td colspan=\"2\" class=\"launchdetails\" style=\"background-image: url('http://www.spaceflightinsider.com/wp-content/plugins/mission-tracking/images/logos/providers_large/ula.png');\"><table><tbody><tr><th>Location</th><td>Cape Canaveral AFS SLC-41</td></tr><tr><th>Time</th><td>TBD</td></tr></tbody></table></td></tr><tr><td colspan=\"2\" class=\"description\"><p>NROL-52 is a classified spacecraft payload for the National Reconnaissance Office (NRO) launching via a United Launch Alliance (ULA) Atlas V in the 421 configuration, indicating the rocket will have four meter fairing, two solid rocket motors, and a single Aerojet Rocketdyne RL10C engine in the Atlas' Centaur upper stage.</p><div></div></td></tr></tbody></table>";
        assertTrue(Parser.parseLaunches(html).isEmpty());
    }

    private void testParsedLaunch(String expectedMissionName, String expectedLocation, ZonedDateTime expectedTime, int expectedWindow, String expectedLaunchVehicle, String expectedDescription, Launch parsedLaunch) {
        assertEquals(expectedMissionName, parsedLaunch.getMissionName());
        assertEquals(expectedLocation, parsedLaunch.getLocation());
        assertEquals(expectedTime, parsedLaunch.getTime());
        assertEquals(expectedWindow, parsedLaunch.getWindow());
        assertEquals(expectedLaunchVehicle, parsedLaunch.getLaunchVehicle());
        assertEquals(expectedDescription, parsedLaunch.getDescription());
    }
}
