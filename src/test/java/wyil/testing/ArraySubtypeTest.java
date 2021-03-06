// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

// This file was automatically generated.
package wyil.testing;
import org.junit.*;

import wybs.util.ResolveError;

import static org.junit.Assert.*;
import wyil.lang.Type;
import wyil.util.TypeSystem;

public class ArraySubtypeTest {
	@Test public void test_1() { checkIsSubtype("any","any"); }
	@Test public void test_2() { checkIsSubtype("any","null"); }
	@Test public void test_3() { checkIsSubtype("any","int"); }
	@Test public void test_9() { checkIsSubtype("any","any[]"); }
	@Test public void test_10() { checkIsSubtype("any","null[]"); }
	@Test public void test_11() { checkIsSubtype("any","int[]"); }
	@Test public void test_25() { checkIsSubtype("any","any[][]"); }
	@Test public void test_26() { checkIsSubtype("any","null[][]"); }
	@Test public void test_27() { checkIsSubtype("any","int[][]"); }
	@Test public void test_28() { checkIsSubtype("any","any"); }
	@Test public void test_29() { checkIsSubtype("any","null"); }
	@Test public void test_30() { checkIsSubtype("any","int"); }
	@Test public void test_31() { checkIsSubtype("any","any"); }
	@Test public void test_32() { checkIsSubtype("any","any"); }
	@Test public void test_33() { checkIsSubtype("any","any"); }
	@Test public void test_34() { checkIsSubtype("any","any"); }
	@Test public void test_35() { checkIsSubtype("any","null"); }
	@Test public void test_36() { checkIsSubtype("any","any"); }
	@Test public void test_37() { checkIsSubtype("any","null"); }
	@Test public void test_38() { checkIsSubtype("any","null|int"); }
	@Test public void test_39() { checkIsSubtype("any","int"); }
	@Test public void test_40() { checkIsSubtype("any","any"); }
	@Test public void test_41() { checkIsSubtype("any","int|null"); }
	@Test public void test_42() { checkIsSubtype("any","int"); }
	@Test public void test_44() { checkIsSubtype("any","any"); }
	@Test public void test_48() { checkIsSubtype("any","any"); }
	@Test public void test_49() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_50() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_51() { checkNotSubtype("null","any"); }
	@Test public void test_52() { checkIsSubtype("null","null"); }
	@Test public void test_53() { checkNotSubtype("null","int"); }
	@Test public void test_59() { checkNotSubtype("null","any[]"); }
	//@Test public void test_60() { checkNotSubtype("null","null[]"); }
	@Test public void test_61() { checkNotSubtype("null","int[]"); }
	@Test public void test_75() { checkNotSubtype("null","any[][]"); }
	//@Test public void test_76() { checkNotSubtype("null","null[][]"); }
	@Test public void test_77() { checkNotSubtype("null","int[][]"); }
	@Test public void test_78() { checkNotSubtype("null","any"); }
	@Test public void test_79() { checkIsSubtype("null","null"); }
	@Test public void test_80() { checkNotSubtype("null","int"); }
	@Test public void test_81() { checkNotSubtype("null","any"); }
	@Test public void test_82() { checkNotSubtype("null","any"); }
	@Test public void test_83() { checkNotSubtype("null","any"); }
	@Test public void test_84() { checkNotSubtype("null","any"); }
	@Test public void test_85() { checkIsSubtype("null","null"); }
	@Test public void test_86() { checkNotSubtype("null","any"); }
	@Test public void test_87() { checkIsSubtype("null","null"); }
	@Test public void test_88() { checkNotSubtype("null","null|int"); }
	@Test public void test_89() { checkNotSubtype("null","int"); }
	@Test public void test_90() { checkNotSubtype("null","any"); }
	@Test public void test_91() { checkNotSubtype("null","int|null"); }
	@Test public void test_92() { checkNotSubtype("null","int"); }
	@Test public void test_94() { checkNotSubtype("null","any"); }
	@Test public void test_98() { checkNotSubtype("null","any"); }
	//@Test public void test_99() { checkNotSubtype("null","null[]|null"); }
	@Test public void test_100() { checkNotSubtype("null","int[]|int"); }
	@Test public void test_101() { checkNotSubtype("int","any"); }
	@Test public void test_102() { checkNotSubtype("int","null"); }
	@Test public void test_103() { checkIsSubtype("int","int"); }
	@Test public void test_109() { checkNotSubtype("int","any[]"); }
	@Test public void test_110() { checkNotSubtype("int","null[]"); }
//	@Test public void test_111() { checkNotSubtype("int","int[]"); }
	@Test public void test_125() { checkNotSubtype("int","any[][]"); }
	@Test public void test_126() { checkNotSubtype("int","null[][]"); }
//	@Test public void test_127() { checkNotSubtype("int","int[][]"); }
	@Test public void test_128() { checkNotSubtype("int","any"); }
	@Test public void test_129() { checkNotSubtype("int","null"); }
	@Test public void test_130() { checkIsSubtype("int","int"); }
	@Test public void test_131() { checkNotSubtype("int","any"); }
	@Test public void test_132() { checkNotSubtype("int","any"); }
	@Test public void test_133() { checkNotSubtype("int","any"); }
	@Test public void test_134() { checkNotSubtype("int","any"); }
	@Test public void test_135() { checkNotSubtype("int","null"); }
	@Test public void test_136() { checkNotSubtype("int","any"); }
	@Test public void test_137() { checkNotSubtype("int","null"); }
	@Test public void test_138() { checkNotSubtype("int","null|int"); }
	@Test public void test_139() { checkIsSubtype("int","int"); }
	@Test public void test_140() { checkNotSubtype("int","any"); }
	@Test public void test_141() { checkNotSubtype("int","int|null"); }
	@Test public void test_142() { checkIsSubtype("int","int"); }
	@Test public void test_144() { checkNotSubtype("int","any"); }
	@Test public void test_148() { checkNotSubtype("int","any"); }
	@Test public void test_149() { checkNotSubtype("int","null[]|null"); }
//	@Test public void test_150() { checkNotSubtype("int","int[]|int"); }
//	@Test public void test_401() { checkNotSubtype("any[]","any"); }
//	@Test public void test_402() { checkNotSubtype("any[]","null"); }
//	@Test public void test_403() { checkNotSubtype("any[]","int"); }
	@Test public void test_409() { checkIsSubtype("any[]","any[]"); }
	@Test public void test_410() { checkIsSubtype("any[]","null[]"); }
	@Test public void test_411() { checkIsSubtype("any[]","int[]"); }
	@Test public void test_425() { checkIsSubtype("any[]","any[][]"); }
	@Test public void test_426() { checkIsSubtype("any[]","null[][]"); }
	@Test public void test_427() { checkIsSubtype("any[]","int[][]"); }
//	@Test public void test_428() { checkNotSubtype("any[]","any"); }
//	@Test public void test_429() { checkNotSubtype("any[]","null"); }
//	@Test public void test_430() { checkNotSubtype("any[]","int"); }
//	@Test public void test_431() { checkNotSubtype("any[]","any"); }
//	@Test public void test_432() { checkNotSubtype("any[]","any"); }
//	@Test public void test_433() { checkNotSubtype("any[]","any"); }
//	@Test public void test_434() { checkNotSubtype("any[]","any"); }
//	@Test public void test_435() { checkNotSubtype("any[]","null"); }
//	@Test public void test_436() { checkNotSubtype("any[]","any"); }
//	@Test public void test_437() { checkNotSubtype("any[]","null"); }
//	@Test public void test_438() { checkNotSubtype("any[]","null|int"); }
//	@Test public void test_439() { checkNotSubtype("any[]","int"); }
//	@Test public void test_440() { checkNotSubtype("any[]","any"); }
//	@Test public void test_441() { checkNotSubtype("any[]","int|null"); }
//	@Test public void test_442() { checkNotSubtype("any[]","int"); }
//	@Test public void test_444() { checkNotSubtype("any[]","any"); }
//	@Test public void test_448() { checkNotSubtype("any[]","any"); }
//	@Test public void test_449() { checkNotSubtype("any[]","null[]|null"); }
//	@Test public void test_450() { checkNotSubtype("any[]","int[]|int"); }
	@Test public void test_451() { checkNotSubtype("null[]","any"); }
//	@Test public void test_452() { checkNotSubtype("null[]","null"); }
	@Test public void test_453() { checkNotSubtype("null[]","int"); }
	@Test public void test_459() { checkNotSubtype("null[]","any[]"); }
	@Test public void test_460() { checkIsSubtype("null[]","null[]"); }
	@Test public void test_461() { checkNotSubtype("null[]","int[]"); }
	@Test public void test_475() { checkNotSubtype("null[]","any[][]"); }
//	@Test public void test_476() { checkNotSubtype("null[]","null[][]"); }
//	@Test public void test_477() { checkNotSubtype("null[]","int[][]"); }
	@Test public void test_478() { checkNotSubtype("null[]","any"); }
//	@Test public void test_479() { checkNotSubtype("null[]","null"); }
	@Test public void test_480() { checkNotSubtype("null[]","int"); }
	@Test public void test_481() { checkNotSubtype("null[]","any"); }
	@Test public void test_482() { checkNotSubtype("null[]","any"); }
	@Test public void test_483() { checkNotSubtype("null[]","any"); }
	@Test public void test_484() { checkNotSubtype("null[]","any"); }
//	@Test public void test_485() { checkNotSubtype("null[]","null"); }
	@Test public void test_486() { checkNotSubtype("null[]","any"); }
//	@Test public void test_487() { checkNotSubtype("null[]","null"); }
	@Test public void test_488() { checkNotSubtype("null[]","null|int"); }
	@Test public void test_489() { checkNotSubtype("null[]","int"); }
	@Test public void test_490() { checkNotSubtype("null[]","any"); }
	@Test public void test_491() { checkNotSubtype("null[]","int|null"); }
	@Test public void test_492() { checkNotSubtype("null[]","int"); }
	@Test public void test_494() { checkNotSubtype("null[]","any"); }
	@Test public void test_498() { checkNotSubtype("null[]","any"); }
//	@Test public void test_499() { checkNotSubtype("null[]","null[]|null"); }
	@Test public void test_500() { checkNotSubtype("null[]","int[]|int"); }
	@Test public void test_501() { checkNotSubtype("int[]","any"); }
	@Test public void test_502() { checkNotSubtype("int[]","null"); }
//	@Test public void test_503() { checkNotSubtype("int[]","int"); }
	@Test public void test_509() { checkNotSubtype("int[]","any[]"); }
	@Test public void test_510() { checkNotSubtype("int[]","null[]"); }
	@Test public void test_511() { checkIsSubtype("int[]","int[]"); }
	@Test public void test_525() { checkNotSubtype("int[]","any[][]"); }
	@Test public void test_526() { checkNotSubtype("int[]","null[][]"); }
//	@Test public void test_527() { checkNotSubtype("int[]","int[][]"); }
	@Test public void test_528() { checkNotSubtype("int[]","any"); }
	@Test public void test_529() { checkNotSubtype("int[]","null"); }
//	@Test public void test_530() { checkNotSubtype("int[]","int"); }
	@Test public void test_531() { checkNotSubtype("int[]","any"); }
	@Test public void test_532() { checkNotSubtype("int[]","any"); }
	@Test public void test_533() { checkNotSubtype("int[]","any"); }
	@Test public void test_534() { checkNotSubtype("int[]","any"); }
	@Test public void test_535() { checkNotSubtype("int[]","null"); }
	@Test public void test_536() { checkNotSubtype("int[]","any"); }
	@Test public void test_537() { checkNotSubtype("int[]","null"); }
	@Test public void test_538() { checkNotSubtype("int[]","null|int"); }
//	@Test public void test_539() { checkNotSubtype("int[]","int"); }
	@Test public void test_540() { checkNotSubtype("int[]","any"); }
	@Test public void test_541() { checkNotSubtype("int[]","int|null"); }
//	@Test public void test_542() { checkNotSubtype("int[]","int"); }
	@Test public void test_544() { checkNotSubtype("int[]","any"); }
	@Test public void test_548() { checkNotSubtype("int[]","any"); }
	@Test public void test_549() { checkNotSubtype("int[]","null[]|null"); }
//	@Test public void test_550() { checkNotSubtype("int[]","int[]|int"); }
//	@Test public void test_1201() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1202() { checkNotSubtype("any[][]","null"); }
//	@Test public void test_1203() { checkNotSubtype("any[][]","int"); }
//	@Test public void test_1209() { checkNotSubtype("any[][]","any[]"); }
//	@Test public void test_1210() { checkNotSubtype("any[][]","null[]"); }
//	@Test public void test_1211() { checkNotSubtype("any[][]","int[]"); }
	@Test public void test_1225() { checkIsSubtype("any[][]","any[][]"); }
	@Test public void test_1226() { checkIsSubtype("any[][]","null[][]"); }
	@Test public void test_1227() { checkIsSubtype("any[][]","int[][]"); }
//	@Test public void test_1228() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1229() { checkNotSubtype("any[][]","null"); }
//	@Test public void test_1230() { checkNotSubtype("any[][]","int"); }
//	@Test public void test_1231() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1232() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1233() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1234() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1235() { checkNotSubtype("any[][]","null"); }
//	@Test public void test_1236() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1237() { checkNotSubtype("any[][]","null"); }
//	@Test public void test_1238() { checkNotSubtype("any[][]","null|int"); }
//	@Test public void test_1239() { checkNotSubtype("any[][]","int"); }
//	@Test public void test_1240() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1241() { checkNotSubtype("any[][]","int|null"); }
//	@Test public void test_1242() { checkNotSubtype("any[][]","int"); }
//	@Test public void test_1244() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1248() { checkNotSubtype("any[][]","any"); }
//	@Test public void test_1249() { checkNotSubtype("any[][]","null[]|null"); }
//	@Test public void test_1250() { checkNotSubtype("any[][]","int[]|int"); }
	@Test public void test_1251() { checkNotSubtype("null[][]","any"); }
//	@Test public void test_1252() { checkNotSubtype("null[][]","null"); }
	@Test public void test_1253() { checkNotSubtype("null[][]","int"); }
	@Test public void test_1259() { checkNotSubtype("null[][]","any[]"); }
//	@Test public void test_1260() { checkNotSubtype("null[][]","null[]"); }
	@Test public void test_1261() { checkNotSubtype("null[][]","int[]"); }
	@Test public void test_1275() { checkNotSubtype("null[][]","any[][]"); }
	@Test public void test_1276() { checkIsSubtype("null[][]","null[][]"); }
	@Test public void test_1277() { checkNotSubtype("null[][]","int[][]"); }
	@Test public void test_1278() { checkNotSubtype("null[][]","any"); }
//	@Test public void test_1279() { checkNotSubtype("null[][]","null"); }
	@Test public void test_1280() { checkNotSubtype("null[][]","int"); }
	@Test public void test_1281() { checkNotSubtype("null[][]","any"); }
	@Test public void test_1282() { checkNotSubtype("null[][]","any"); }
	@Test public void test_1283() { checkNotSubtype("null[][]","any"); }
	@Test public void test_1284() { checkNotSubtype("null[][]","any"); }
//	@Test public void test_1285() { checkNotSubtype("null[][]","null"); }
	@Test public void test_1286() { checkNotSubtype("null[][]","any"); }
//	@Test public void test_1287() { checkNotSubtype("null[][]","null"); }
	@Test public void test_1288() { checkNotSubtype("null[][]","null|int"); }
	@Test public void test_1289() { checkNotSubtype("null[][]","int"); }
	@Test public void test_1290() { checkNotSubtype("null[][]","any"); }
	@Test public void test_1291() { checkNotSubtype("null[][]","int|null"); }
	@Test public void test_1292() { checkNotSubtype("null[][]","int"); }
	@Test public void test_1294() { checkNotSubtype("null[][]","any"); }
	@Test public void test_1298() { checkNotSubtype("null[][]","any"); }
//	@Test public void test_1299() { checkNotSubtype("null[][]","null[]|null"); }
	@Test public void test_1300() { checkNotSubtype("null[][]","int[]|int"); }
	@Test public void test_1301() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1302() { checkNotSubtype("int[][]","null"); }
//	@Test public void test_1303() { checkNotSubtype("int[][]","int"); }
	@Test public void test_1309() { checkNotSubtype("int[][]","any[]"); }
	@Test public void test_1310() { checkNotSubtype("int[][]","null[]"); }
//	@Test public void test_1311() { checkNotSubtype("int[][]","int[]"); }
	@Test public void test_1325() { checkNotSubtype("int[][]","any[][]"); }
	@Test public void test_1326() { checkNotSubtype("int[][]","null[][]"); }
	@Test public void test_1327() { checkIsSubtype("int[][]","int[][]"); }
	@Test public void test_1328() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1329() { checkNotSubtype("int[][]","null"); }
//	@Test public void test_1330() { checkNotSubtype("int[][]","int"); }
	@Test public void test_1331() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1332() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1333() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1334() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1335() { checkNotSubtype("int[][]","null"); }
	@Test public void test_1336() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1337() { checkNotSubtype("int[][]","null"); }
	@Test public void test_1338() { checkNotSubtype("int[][]","null|int"); }
//	@Test public void test_1339() { checkNotSubtype("int[][]","int"); }
	@Test public void test_1340() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1341() { checkNotSubtype("int[][]","int|null"); }
//	@Test public void test_1342() { checkNotSubtype("int[][]","int"); }
	@Test public void test_1344() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1348() { checkNotSubtype("int[][]","any"); }
	@Test public void test_1349() { checkNotSubtype("int[][]","null[]|null"); }
//	@Test public void test_1350() { checkNotSubtype("int[][]","int[]|int"); }
	@Test public void test_1351() { checkIsSubtype("any","any"); }
	@Test public void test_1352() { checkIsSubtype("any","null"); }
	@Test public void test_1353() { checkIsSubtype("any","int"); }
	@Test public void test_1359() { checkIsSubtype("any","any[]"); }
	@Test public void test_1360() { checkIsSubtype("any","null[]"); }
	@Test public void test_1361() { checkIsSubtype("any","int[]"); }
	@Test public void test_1375() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1376() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1377() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1378() { checkIsSubtype("any","any"); }
	@Test public void test_1379() { checkIsSubtype("any","null"); }
	@Test public void test_1380() { checkIsSubtype("any","int"); }
	@Test public void test_1381() { checkIsSubtype("any","any"); }
	@Test public void test_1382() { checkIsSubtype("any","any"); }
	@Test public void test_1383() { checkIsSubtype("any","any"); }
	@Test public void test_1384() { checkIsSubtype("any","any"); }
	@Test public void test_1385() { checkIsSubtype("any","null"); }
	@Test public void test_1386() { checkIsSubtype("any","any"); }
	@Test public void test_1387() { checkIsSubtype("any","null"); }
	@Test public void test_1388() { checkIsSubtype("any","null|int"); }
	@Test public void test_1389() { checkIsSubtype("any","int"); }
	@Test public void test_1390() { checkIsSubtype("any","any"); }
	@Test public void test_1391() { checkIsSubtype("any","int|null"); }
	@Test public void test_1392() { checkIsSubtype("any","int"); }
	@Test public void test_1394() { checkIsSubtype("any","any"); }
	@Test public void test_1398() { checkIsSubtype("any","any"); }
	@Test public void test_1399() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1400() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1401() { checkNotSubtype("null","any"); }
	@Test public void test_1402() { checkIsSubtype("null","null"); }
	@Test public void test_1403() { checkNotSubtype("null","int"); }
	@Test public void test_1409() { checkNotSubtype("null","any[]"); }
//	@Test public void test_1410() { checkNotSubtype("null","null[]"); }
	@Test public void test_1411() { checkNotSubtype("null","int[]"); }
	@Test public void test_1425() { checkNotSubtype("null","any[][]"); }
//	@Test public void test_1426() { checkNotSubtype("null","null[][]"); }
	@Test public void test_1427() { checkNotSubtype("null","int[][]"); }
	@Test public void test_1428() { checkNotSubtype("null","any"); }
	@Test public void test_1429() { checkIsSubtype("null","null"); }
	@Test public void test_1430() { checkNotSubtype("null","int"); }
	@Test public void test_1431() { checkNotSubtype("null","any"); }
	@Test public void test_1432() { checkNotSubtype("null","any"); }
	@Test public void test_1433() { checkNotSubtype("null","any"); }
	@Test public void test_1434() { checkNotSubtype("null","any"); }
	@Test public void test_1435() { checkIsSubtype("null","null"); }
	@Test public void test_1436() { checkNotSubtype("null","any"); }
	@Test public void test_1437() { checkIsSubtype("null","null"); }
	@Test public void test_1438() { checkNotSubtype("null","null|int"); }
	@Test public void test_1439() { checkNotSubtype("null","int"); }
	@Test public void test_1440() { checkNotSubtype("null","any"); }
	@Test public void test_1441() { checkNotSubtype("null","int|null"); }
	@Test public void test_1442() { checkNotSubtype("null","int"); }
	@Test public void test_1444() { checkNotSubtype("null","any"); }
	@Test public void test_1448() { checkNotSubtype("null","any"); }
//	@Test public void test_1449() { checkNotSubtype("null","null[]|null"); }
	@Test public void test_1450() { checkNotSubtype("null","int[]|int"); }
	@Test public void test_1451() { checkNotSubtype("int","any"); }
	@Test public void test_1452() { checkNotSubtype("int","null"); }
	@Test public void test_1453() { checkIsSubtype("int","int"); }
	@Test public void test_1459() { checkNotSubtype("int","any[]"); }
	@Test public void test_1460() { checkNotSubtype("int","null[]"); }
//	@Test public void test_1461() { checkNotSubtype("int","int[]"); }
	@Test public void test_1475() { checkNotSubtype("int","any[][]"); }
	@Test public void test_1476() { checkNotSubtype("int","null[][]"); }
//	@Test public void test_1477() { checkNotSubtype("int","int[][]"); }
	@Test public void test_1478() { checkNotSubtype("int","any"); }
	@Test public void test_1479() { checkNotSubtype("int","null"); }
	@Test public void test_1480() { checkIsSubtype("int","int"); }
	@Test public void test_1481() { checkNotSubtype("int","any"); }
	@Test public void test_1482() { checkNotSubtype("int","any"); }
	@Test public void test_1483() { checkNotSubtype("int","any"); }
	@Test public void test_1484() { checkNotSubtype("int","any"); }
	@Test public void test_1485() { checkNotSubtype("int","null"); }
	@Test public void test_1486() { checkNotSubtype("int","any"); }
	@Test public void test_1487() { checkNotSubtype("int","null"); }
	@Test public void test_1488() { checkNotSubtype("int","null|int"); }
	@Test public void test_1489() { checkIsSubtype("int","int"); }
	@Test public void test_1490() { checkNotSubtype("int","any"); }
	@Test public void test_1491() { checkNotSubtype("int","int|null"); }
	@Test public void test_1492() { checkIsSubtype("int","int"); }
	@Test public void test_1494() { checkNotSubtype("int","any"); }
	@Test public void test_1498() { checkNotSubtype("int","any"); }
	@Test public void test_1499() { checkNotSubtype("int","null[]|null"); }
//	@Test public void test_1500() { checkNotSubtype("int","int[]|int"); }
	@Test public void test_1501() { checkIsSubtype("any","any"); }
	@Test public void test_1502() { checkIsSubtype("any","null"); }
	@Test public void test_1503() { checkIsSubtype("any","int"); }
	@Test public void test_1509() { checkIsSubtype("any","any[]"); }
	@Test public void test_1510() { checkIsSubtype("any","null[]"); }
	@Test public void test_1511() { checkIsSubtype("any","int[]"); }
	@Test public void test_1525() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1526() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1527() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1528() { checkIsSubtype("any","any"); }
	@Test public void test_1529() { checkIsSubtype("any","null"); }
	@Test public void test_1530() { checkIsSubtype("any","int"); }
	@Test public void test_1531() { checkIsSubtype("any","any"); }
	@Test public void test_1532() { checkIsSubtype("any","any"); }
	@Test public void test_1533() { checkIsSubtype("any","any"); }
	@Test public void test_1534() { checkIsSubtype("any","any"); }
	@Test public void test_1535() { checkIsSubtype("any","null"); }
	@Test public void test_1536() { checkIsSubtype("any","any"); }
	@Test public void test_1537() { checkIsSubtype("any","null"); }
	@Test public void test_1538() { checkIsSubtype("any","null|int"); }
	@Test public void test_1539() { checkIsSubtype("any","int"); }
	@Test public void test_1540() { checkIsSubtype("any","any"); }
	@Test public void test_1541() { checkIsSubtype("any","int|null"); }
	@Test public void test_1542() { checkIsSubtype("any","int"); }
	@Test public void test_1544() { checkIsSubtype("any","any"); }
	@Test public void test_1548() { checkIsSubtype("any","any"); }
	@Test public void test_1549() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1550() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1551() { checkIsSubtype("any","any"); }
	@Test public void test_1552() { checkIsSubtype("any","null"); }
	@Test public void test_1553() { checkIsSubtype("any","int"); }
	@Test public void test_1559() { checkIsSubtype("any","any[]"); }
	@Test public void test_1560() { checkIsSubtype("any","null[]"); }
	@Test public void test_1561() { checkIsSubtype("any","int[]"); }
	@Test public void test_1575() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1576() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1577() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1578() { checkIsSubtype("any","any"); }
	@Test public void test_1579() { checkIsSubtype("any","null"); }
	@Test public void test_1580() { checkIsSubtype("any","int"); }
	@Test public void test_1581() { checkIsSubtype("any","any"); }
	@Test public void test_1582() { checkIsSubtype("any","any"); }
	@Test public void test_1583() { checkIsSubtype("any","any"); }
	@Test public void test_1584() { checkIsSubtype("any","any"); }
	@Test public void test_1585() { checkIsSubtype("any","null"); }
	@Test public void test_1586() { checkIsSubtype("any","any"); }
	@Test public void test_1587() { checkIsSubtype("any","null"); }
	@Test public void test_1588() { checkIsSubtype("any","null|int"); }
	@Test public void test_1589() { checkIsSubtype("any","int"); }
	@Test public void test_1590() { checkIsSubtype("any","any"); }
	@Test public void test_1591() { checkIsSubtype("any","int|null"); }
	@Test public void test_1592() { checkIsSubtype("any","int"); }
	@Test public void test_1594() { checkIsSubtype("any","any"); }
	@Test public void test_1598() { checkIsSubtype("any","any"); }
	@Test public void test_1599() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1600() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1601() { checkIsSubtype("any","any"); }
	@Test public void test_1602() { checkIsSubtype("any","null"); }
	@Test public void test_1603() { checkIsSubtype("any","int"); }
	@Test public void test_1609() { checkIsSubtype("any","any[]"); }
	@Test public void test_1610() { checkIsSubtype("any","null[]"); }
	@Test public void test_1611() { checkIsSubtype("any","int[]"); }
	@Test public void test_1625() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1626() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1627() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1628() { checkIsSubtype("any","any"); }
	@Test public void test_1629() { checkIsSubtype("any","null"); }
	@Test public void test_1630() { checkIsSubtype("any","int"); }
	@Test public void test_1631() { checkIsSubtype("any","any"); }
	@Test public void test_1632() { checkIsSubtype("any","any"); }
	@Test public void test_1633() { checkIsSubtype("any","any"); }
	@Test public void test_1634() { checkIsSubtype("any","any"); }
	@Test public void test_1635() { checkIsSubtype("any","null"); }
	@Test public void test_1636() { checkIsSubtype("any","any"); }
	@Test public void test_1637() { checkIsSubtype("any","null"); }
	@Test public void test_1638() { checkIsSubtype("any","null|int"); }
	@Test public void test_1639() { checkIsSubtype("any","int"); }
	@Test public void test_1640() { checkIsSubtype("any","any"); }
	@Test public void test_1641() { checkIsSubtype("any","int|null"); }
	@Test public void test_1642() { checkIsSubtype("any","int"); }
	@Test public void test_1644() { checkIsSubtype("any","any"); }
	@Test public void test_1648() { checkIsSubtype("any","any"); }
	@Test public void test_1649() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1650() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1651() { checkIsSubtype("any","any"); }
	@Test public void test_1652() { checkIsSubtype("any","null"); }
	@Test public void test_1653() { checkIsSubtype("any","int"); }
	@Test public void test_1659() { checkIsSubtype("any","any[]"); }
	@Test public void test_1660() { checkIsSubtype("any","null[]"); }
	@Test public void test_1661() { checkIsSubtype("any","int[]"); }
	@Test public void test_1675() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1676() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1677() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1678() { checkIsSubtype("any","any"); }
	@Test public void test_1679() { checkIsSubtype("any","null"); }
	@Test public void test_1680() { checkIsSubtype("any","int"); }
	@Test public void test_1681() { checkIsSubtype("any","any"); }
	@Test public void test_1682() { checkIsSubtype("any","any"); }
	@Test public void test_1683() { checkIsSubtype("any","any"); }
	@Test public void test_1684() { checkIsSubtype("any","any"); }
	@Test public void test_1685() { checkIsSubtype("any","null"); }
	@Test public void test_1686() { checkIsSubtype("any","any"); }
	@Test public void test_1687() { checkIsSubtype("any","null"); }
	@Test public void test_1688() { checkIsSubtype("any","null|int"); }
	@Test public void test_1689() { checkIsSubtype("any","int"); }
	@Test public void test_1690() { checkIsSubtype("any","any"); }
	@Test public void test_1691() { checkIsSubtype("any","int|null"); }
	@Test public void test_1692() { checkIsSubtype("any","int"); }
	@Test public void test_1694() { checkIsSubtype("any","any"); }
	@Test public void test_1698() { checkIsSubtype("any","any"); }
	@Test public void test_1699() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1700() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1701() { checkNotSubtype("null","any"); }
	@Test public void test_1702() { checkIsSubtype("null","null"); }
	@Test public void test_1703() { checkNotSubtype("null","int"); }
	@Test public void test_1709() { checkNotSubtype("null","any[]"); }
//	@Test public void test_1710() { checkNotSubtype("null","null[]"); }
	@Test public void test_1711() { checkNotSubtype("null","int[]"); }
	@Test public void test_1725() { checkNotSubtype("null","any[][]"); }
//	@Test public void test_1726() { checkNotSubtype("null","null[][]"); }
	@Test public void test_1727() { checkNotSubtype("null","int[][]"); }
	@Test public void test_1728() { checkNotSubtype("null","any"); }
	@Test public void test_1729() { checkIsSubtype("null","null"); }
	@Test public void test_1730() { checkNotSubtype("null","int"); }
	@Test public void test_1731() { checkNotSubtype("null","any"); }
	@Test public void test_1732() { checkNotSubtype("null","any"); }
	@Test public void test_1733() { checkNotSubtype("null","any"); }
	@Test public void test_1734() { checkNotSubtype("null","any"); }
	@Test public void test_1735() { checkIsSubtype("null","null"); }
	@Test public void test_1736() { checkNotSubtype("null","any"); }
	@Test public void test_1737() { checkIsSubtype("null","null"); }
	@Test public void test_1738() { checkNotSubtype("null","null|int"); }
	@Test public void test_1739() { checkNotSubtype("null","int"); }
	@Test public void test_1740() { checkNotSubtype("null","any"); }
	@Test public void test_1741() { checkNotSubtype("null","int|null"); }
	@Test public void test_1742() { checkNotSubtype("null","int"); }
	@Test public void test_1744() { checkNotSubtype("null","any"); }
	@Test public void test_1748() { checkNotSubtype("null","any"); }
//	@Test public void test_1749() { checkNotSubtype("null","null[]|null"); }
	@Test public void test_1750() { checkNotSubtype("null","int[]|int"); }
	@Test public void test_1751() { checkIsSubtype("any","any"); }
	@Test public void test_1752() { checkIsSubtype("any","null"); }
	@Test public void test_1753() { checkIsSubtype("any","int"); }
	@Test public void test_1759() { checkIsSubtype("any","any[]"); }
	@Test public void test_1760() { checkIsSubtype("any","null[]"); }
	@Test public void test_1761() { checkIsSubtype("any","int[]"); }
	@Test public void test_1775() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1776() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1777() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1778() { checkIsSubtype("any","any"); }
	@Test public void test_1779() { checkIsSubtype("any","null"); }
	@Test public void test_1780() { checkIsSubtype("any","int"); }
	@Test public void test_1781() { checkIsSubtype("any","any"); }
	@Test public void test_1782() { checkIsSubtype("any","any"); }
	@Test public void test_1783() { checkIsSubtype("any","any"); }
	@Test public void test_1784() { checkIsSubtype("any","any"); }
	@Test public void test_1785() { checkIsSubtype("any","null"); }
	@Test public void test_1786() { checkIsSubtype("any","any"); }
	@Test public void test_1787() { checkIsSubtype("any","null"); }
	@Test public void test_1788() { checkIsSubtype("any","null|int"); }
	@Test public void test_1789() { checkIsSubtype("any","int"); }
	@Test public void test_1790() { checkIsSubtype("any","any"); }
	@Test public void test_1791() { checkIsSubtype("any","int|null"); }
	@Test public void test_1792() { checkIsSubtype("any","int"); }
	@Test public void test_1794() { checkIsSubtype("any","any"); }
	@Test public void test_1798() { checkIsSubtype("any","any"); }
	@Test public void test_1799() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_1800() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_1801() { checkNotSubtype("null","any"); }
	@Test public void test_1802() { checkIsSubtype("null","null"); }
	@Test public void test_1803() { checkNotSubtype("null","int"); }
	@Test public void test_1809() { checkNotSubtype("null","any[]"); }
//	@Test public void test_1810() { checkNotSubtype("null","null[]"); }
	@Test public void test_1811() { checkNotSubtype("null","int[]"); }
	@Test public void test_1825() { checkNotSubtype("null","any[][]"); }
//	@Test public void test_1826() { checkNotSubtype("null","null[][]"); }
	@Test public void test_1827() { checkNotSubtype("null","int[][]"); }
	@Test public void test_1828() { checkNotSubtype("null","any"); }
	@Test public void test_1829() { checkIsSubtype("null","null"); }
	@Test public void test_1830() { checkNotSubtype("null","int"); }
	@Test public void test_1831() { checkNotSubtype("null","any"); }
	@Test public void test_1832() { checkNotSubtype("null","any"); }
	@Test public void test_1833() { checkNotSubtype("null","any"); }
	@Test public void test_1834() { checkNotSubtype("null","any"); }
	@Test public void test_1835() { checkIsSubtype("null","null"); }
	@Test public void test_1836() { checkNotSubtype("null","any"); }
	@Test public void test_1837() { checkIsSubtype("null","null"); }
	@Test public void test_1838() { checkNotSubtype("null","null|int"); }
	@Test public void test_1839() { checkNotSubtype("null","int"); }
	@Test public void test_1840() { checkNotSubtype("null","any"); }
	@Test public void test_1841() { checkNotSubtype("null","int|null"); }
	@Test public void test_1842() { checkNotSubtype("null","int"); }
	@Test public void test_1844() { checkNotSubtype("null","any"); }
	@Test public void test_1848() { checkNotSubtype("null","any"); }
//	@Test public void test_1849() { checkNotSubtype("null","null[]|null"); }
	@Test public void test_1850() { checkNotSubtype("null","int[]|int"); }
	@Test public void test_1851() { checkNotSubtype("null|int","any"); }
	@Test public void test_1852() { checkIsSubtype("null|int","null"); }
	@Test public void test_1853() { checkIsSubtype("null|int","int"); }
	@Test public void test_1859() { checkNotSubtype("null|int","any[]"); }
//	@Test public void test_1860() { checkNotSubtype("null|int","null[]"); }
//	@Test public void test_1861() { checkNotSubtype("null|int","int[]"); }
	@Test public void test_1875() { checkNotSubtype("null|int","any[][]"); }
//	@Test public void test_1876() { checkNotSubtype("null|int","null[][]"); }
//	@Test public void test_1877() { checkNotSubtype("null|int","int[][]"); }
	@Test public void test_1878() { checkNotSubtype("null|int","any"); }
	@Test public void test_1879() { checkIsSubtype("null|int","null"); }
	@Test public void test_1880() { checkIsSubtype("null|int","int"); }
	@Test public void test_1881() { checkNotSubtype("null|int","any"); }
	@Test public void test_1882() { checkNotSubtype("null|int","any"); }
	@Test public void test_1883() { checkNotSubtype("null|int","any"); }
	@Test public void test_1884() { checkNotSubtype("null|int","any"); }
	@Test public void test_1885() { checkIsSubtype("null|int","null"); }
	@Test public void test_1886() { checkNotSubtype("null|int","any"); }
	@Test public void test_1887() { checkIsSubtype("null|int","null"); }
	@Test public void test_1888() { checkIsSubtype("null|int","null|int"); }
	@Test public void test_1889() { checkIsSubtype("null|int","int"); }
	@Test public void test_1890() { checkNotSubtype("null|int","any"); }
	@Test public void test_1891() { checkIsSubtype("null|int","int|null"); }
	@Test public void test_1892() { checkIsSubtype("null|int","int"); }
	@Test public void test_1894() { checkNotSubtype("null|int","any"); }
	@Test public void test_1898() { checkNotSubtype("null|int","any"); }
//	@Test public void test_1899() { checkNotSubtype("null|int","null[]|null"); }
//	@Test public void test_1900() { checkNotSubtype("null|int","int[]|int"); }
	@Test public void test_1901() { checkNotSubtype("int","any"); }
	@Test public void test_1902() { checkNotSubtype("int","null"); }
	@Test public void test_1903() { checkIsSubtype("int","int"); }
	@Test public void test_1909() { checkNotSubtype("int","any[]"); }
	@Test public void test_1910() { checkNotSubtype("int","null[]"); }
//	@Test public void test_1911() { checkNotSubtype("int","int[]"); }
	@Test public void test_1925() { checkNotSubtype("int","any[][]"); }
	@Test public void test_1926() { checkNotSubtype("int","null[][]"); }
//	@Test public void test_1927() { checkNotSubtype("int","int[][]"); }
	@Test public void test_1928() { checkNotSubtype("int","any"); }
	@Test public void test_1929() { checkNotSubtype("int","null"); }
	@Test public void test_1930() { checkIsSubtype("int","int"); }
	@Test public void test_1931() { checkNotSubtype("int","any"); }
	@Test public void test_1932() { checkNotSubtype("int","any"); }
	@Test public void test_1933() { checkNotSubtype("int","any"); }
	@Test public void test_1934() { checkNotSubtype("int","any"); }
	@Test public void test_1935() { checkNotSubtype("int","null"); }
	@Test public void test_1936() { checkNotSubtype("int","any"); }
	@Test public void test_1937() { checkNotSubtype("int","null"); }
	@Test public void test_1938() { checkNotSubtype("int","null|int"); }
	@Test public void test_1939() { checkIsSubtype("int","int"); }
	@Test public void test_1940() { checkNotSubtype("int","any"); }
	@Test public void test_1941() { checkNotSubtype("int","int|null"); }
	@Test public void test_1942() { checkIsSubtype("int","int"); }
	@Test public void test_1944() { checkNotSubtype("int","any"); }
	@Test public void test_1948() { checkNotSubtype("int","any"); }
	@Test public void test_1949() { checkNotSubtype("int","null[]|null"); }
//	@Test public void test_1950() { checkNotSubtype("int","int[]|int"); }
	@Test public void test_1951() { checkIsSubtype("any","any"); }
	@Test public void test_1952() { checkIsSubtype("any","null"); }
	@Test public void test_1953() { checkIsSubtype("any","int"); }
	@Test public void test_1959() { checkIsSubtype("any","any[]"); }
	@Test public void test_1960() { checkIsSubtype("any","null[]"); }
	@Test public void test_1961() { checkIsSubtype("any","int[]"); }
	@Test public void test_1975() { checkIsSubtype("any","any[][]"); }
	@Test public void test_1976() { checkIsSubtype("any","null[][]"); }
	@Test public void test_1977() { checkIsSubtype("any","int[][]"); }
	@Test public void test_1978() { checkIsSubtype("any","any"); }
	@Test public void test_1979() { checkIsSubtype("any","null"); }
	@Test public void test_1980() { checkIsSubtype("any","int"); }
	@Test public void test_1981() { checkIsSubtype("any","any"); }
	@Test public void test_1982() { checkIsSubtype("any","any"); }
	@Test public void test_1983() { checkIsSubtype("any","any"); }
	@Test public void test_1984() { checkIsSubtype("any","any"); }
	@Test public void test_1985() { checkIsSubtype("any","null"); }
	@Test public void test_1986() { checkIsSubtype("any","any"); }
	@Test public void test_1987() { checkIsSubtype("any","null"); }
	@Test public void test_1988() { checkIsSubtype("any","null|int"); }
	@Test public void test_1989() { checkIsSubtype("any","int"); }
	@Test public void test_1990() { checkIsSubtype("any","any"); }
	@Test public void test_1991() { checkIsSubtype("any","int|null"); }
	@Test public void test_1992() { checkIsSubtype("any","int"); }
	@Test public void test_1994() { checkIsSubtype("any","any"); }
	@Test public void test_1998() { checkIsSubtype("any","any"); }
	@Test public void test_1999() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_2000() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_2001() { checkNotSubtype("int|null","any"); }
	@Test public void test_2002() { checkIsSubtype("int|null","null"); }
	@Test public void test_2003() { checkIsSubtype("int|null","int"); }
	@Test public void test_2009() { checkNotSubtype("int|null","any[]"); }
//	@Test public void test_2010() { checkNotSubtype("int|null","null[]"); }
//	@Test public void test_2011() { checkNotSubtype("int|null","int[]"); }
	@Test public void test_2025() { checkNotSubtype("int|null","any[][]"); }
//	@Test public void test_2026() { checkNotSubtype("int|null","null[][]"); }
//	@Test public void test_2027() { checkNotSubtype("int|null","int[][]"); }
	@Test public void test_2028() { checkNotSubtype("int|null","any"); }
	@Test public void test_2029() { checkIsSubtype("int|null","null"); }
	@Test public void test_2030() { checkIsSubtype("int|null","int"); }
	@Test public void test_2031() { checkNotSubtype("int|null","any"); }
	@Test public void test_2032() { checkNotSubtype("int|null","any"); }
	@Test public void test_2033() { checkNotSubtype("int|null","any"); }
	@Test public void test_2034() { checkNotSubtype("int|null","any"); }
	@Test public void test_2035() { checkIsSubtype("int|null","null"); }
	@Test public void test_2036() { checkNotSubtype("int|null","any"); }
	@Test public void test_2037() { checkIsSubtype("int|null","null"); }
	@Test public void test_2038() { checkIsSubtype("int|null","null|int"); }
	@Test public void test_2039() { checkIsSubtype("int|null","int"); }
	@Test public void test_2040() { checkNotSubtype("int|null","any"); }
	@Test public void test_2041() { checkIsSubtype("int|null","int|null"); }
	@Test public void test_2042() { checkIsSubtype("int|null","int"); }
	@Test public void test_2044() { checkNotSubtype("int|null","any"); }
	@Test public void test_2048() { checkNotSubtype("int|null","any"); }
//	@Test public void test_2049() { checkNotSubtype("int|null","null[]|null"); }
//	@Test public void test_2050() { checkNotSubtype("int|null","int[]|int"); }
	@Test public void test_2051() { checkNotSubtype("int","any"); }
	@Test public void test_2052() { checkNotSubtype("int","null"); }
	@Test public void test_2053() { checkIsSubtype("int","int"); }
	@Test public void test_2059() { checkNotSubtype("int","any[]"); }
	@Test public void test_2060() { checkNotSubtype("int","null[]"); }
//	@Test public void test_2061() { checkNotSubtype("int","int[]"); }
	@Test public void test_2075() { checkNotSubtype("int","any[][]"); }
	@Test public void test_2076() { checkNotSubtype("int","null[][]"); }
//	@Test public void test_2077() { checkNotSubtype("int","int[][]"); }
	@Test public void test_2078() { checkNotSubtype("int","any"); }
	@Test public void test_2079() { checkNotSubtype("int","null"); }
	@Test public void test_2080() { checkIsSubtype("int","int"); }
	@Test public void test_2081() { checkNotSubtype("int","any"); }
	@Test public void test_2082() { checkNotSubtype("int","any"); }
	@Test public void test_2083() { checkNotSubtype("int","any"); }
	@Test public void test_2084() { checkNotSubtype("int","any"); }
	@Test public void test_2085() { checkNotSubtype("int","null"); }
	@Test public void test_2086() { checkNotSubtype("int","any"); }
	@Test public void test_2087() { checkNotSubtype("int","null"); }
	@Test public void test_2088() { checkNotSubtype("int","null|int"); }
	@Test public void test_2089() { checkIsSubtype("int","int"); }
	@Test public void test_2090() { checkNotSubtype("int","any"); }
	@Test public void test_2091() { checkNotSubtype("int","int|null"); }
	@Test public void test_2092() { checkIsSubtype("int","int"); }
	@Test public void test_2094() { checkNotSubtype("int","any"); }
	@Test public void test_2098() { checkNotSubtype("int","any"); }
	@Test public void test_2099() { checkNotSubtype("int","null[]|null"); }
//	@Test public void test_2100() { checkNotSubtype("int","int[]|int"); }
	@Test public void test_2151() { checkIsSubtype("any","any"); }
	@Test public void test_2152() { checkIsSubtype("any","null"); }
	@Test public void test_2153() { checkIsSubtype("any","int"); }
	@Test public void test_2159() { checkIsSubtype("any","any[]"); }
	@Test public void test_2160() { checkIsSubtype("any","null[]"); }
	@Test public void test_2161() { checkIsSubtype("any","int[]"); }
	@Test public void test_2175() { checkIsSubtype("any","any[][]"); }
	@Test public void test_2176() { checkIsSubtype("any","null[][]"); }
	@Test public void test_2177() { checkIsSubtype("any","int[][]"); }
	@Test public void test_2178() { checkIsSubtype("any","any"); }
	@Test public void test_2179() { checkIsSubtype("any","null"); }
	@Test public void test_2180() { checkIsSubtype("any","int"); }
	@Test public void test_2181() { checkIsSubtype("any","any"); }
	@Test public void test_2182() { checkIsSubtype("any","any"); }
	@Test public void test_2183() { checkIsSubtype("any","any"); }
	@Test public void test_2184() { checkIsSubtype("any","any"); }
	@Test public void test_2185() { checkIsSubtype("any","null"); }
	@Test public void test_2186() { checkIsSubtype("any","any"); }
	@Test public void test_2187() { checkIsSubtype("any","null"); }
	@Test public void test_2188() { checkIsSubtype("any","null|int"); }
	@Test public void test_2189() { checkIsSubtype("any","int"); }
	@Test public void test_2190() { checkIsSubtype("any","any"); }
	@Test public void test_2191() { checkIsSubtype("any","int|null"); }
	@Test public void test_2192() { checkIsSubtype("any","int"); }
	@Test public void test_2194() { checkIsSubtype("any","any"); }
	@Test public void test_2198() { checkIsSubtype("any","any"); }
	@Test public void test_2199() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_2200() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_2351() { checkIsSubtype("any","any"); }
	@Test public void test_2352() { checkIsSubtype("any","null"); }
	@Test public void test_2353() { checkIsSubtype("any","int"); }
	@Test public void test_2359() { checkIsSubtype("any","any[]"); }
	@Test public void test_2360() { checkIsSubtype("any","null[]"); }
	@Test public void test_2361() { checkIsSubtype("any","int[]"); }
	@Test public void test_2375() { checkIsSubtype("any","any[][]"); }
	@Test public void test_2376() { checkIsSubtype("any","null[][]"); }
	@Test public void test_2377() { checkIsSubtype("any","int[][]"); }
	@Test public void test_2378() { checkIsSubtype("any","any"); }
	@Test public void test_2379() { checkIsSubtype("any","null"); }
	@Test public void test_2380() { checkIsSubtype("any","int"); }
	@Test public void test_2381() { checkIsSubtype("any","any"); }
	@Test public void test_2382() { checkIsSubtype("any","any"); }
	@Test public void test_2383() { checkIsSubtype("any","any"); }
	@Test public void test_2384() { checkIsSubtype("any","any"); }
	@Test public void test_2385() { checkIsSubtype("any","null"); }
	@Test public void test_2386() { checkIsSubtype("any","any"); }
	@Test public void test_2387() { checkIsSubtype("any","null"); }
	@Test public void test_2388() { checkIsSubtype("any","null|int"); }
	@Test public void test_2389() { checkIsSubtype("any","int"); }
	@Test public void test_2390() { checkIsSubtype("any","any"); }
	@Test public void test_2391() { checkIsSubtype("any","int|null"); }
	@Test public void test_2392() { checkIsSubtype("any","int"); }
	@Test public void test_2394() { checkIsSubtype("any","any"); }
	@Test public void test_2398() { checkIsSubtype("any","any"); }
	@Test public void test_2399() { checkIsSubtype("any","null[]|null"); }
	@Test public void test_2400() { checkIsSubtype("any","int[]|int"); }
	@Test public void test_2401() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2402() { checkIsSubtype("null[]|null","null"); }
	@Test public void test_2403() { checkNotSubtype("null[]|null","int"); }
	@Test public void test_2409() { checkNotSubtype("null[]|null","any[]"); }
	@Test public void test_2410() { checkIsSubtype("null[]|null","null[]"); }
	@Test public void test_2411() { checkNotSubtype("null[]|null","int[]"); }
	@Test public void test_2425() { checkNotSubtype("null[]|null","any[][]"); }
//	@Test public void test_2426() { checkNotSubtype("null[]|null","null[][]"); }
	@Test public void test_2427() { checkNotSubtype("null[]|null","int[][]"); }
	@Test public void test_2428() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2429() { checkIsSubtype("null[]|null","null"); }
	@Test public void test_2430() { checkNotSubtype("null[]|null","int"); }
	@Test public void test_2431() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2432() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2433() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2434() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2435() { checkIsSubtype("null[]|null","null"); }
	@Test public void test_2436() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2437() { checkIsSubtype("null[]|null","null"); }
	@Test public void test_2438() { checkNotSubtype("null[]|null","null|int"); }
	@Test public void test_2439() { checkNotSubtype("null[]|null","int"); }
	@Test public void test_2440() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2441() { checkNotSubtype("null[]|null","int|null"); }
	@Test public void test_2442() { checkNotSubtype("null[]|null","int"); }
	@Test public void test_2444() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2448() { checkNotSubtype("null[]|null","any"); }
	@Test public void test_2449() { checkIsSubtype("null[]|null","null[]|null"); }
	@Test public void test_2450() { checkNotSubtype("null[]|null","int[]|int"); }
	@Test public void test_2451() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2452() { checkNotSubtype("int[]|int","null"); }
	@Test public void test_2453() { checkIsSubtype("int[]|int","int"); }
	@Test public void test_2459() { checkNotSubtype("int[]|int","any[]"); }
	@Test public void test_2460() { checkNotSubtype("int[]|int","null[]"); }
	@Test public void test_2461() { checkIsSubtype("int[]|int","int[]"); }
	@Test public void test_2475() { checkNotSubtype("int[]|int","any[][]"); }
	@Test public void test_2476() { checkNotSubtype("int[]|int","null[][]"); }
//	@Test public void test_2477() { checkNotSubtype("int[]|int","int[][]"); }
	@Test public void test_2478() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2479() { checkNotSubtype("int[]|int","null"); }
	@Test public void test_2480() { checkIsSubtype("int[]|int","int"); }
	@Test public void test_2481() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2482() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2483() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2484() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2485() { checkNotSubtype("int[]|int","null"); }
	@Test public void test_2486() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2487() { checkNotSubtype("int[]|int","null"); }
	@Test public void test_2488() { checkNotSubtype("int[]|int","null|int"); }
	@Test public void test_2489() { checkIsSubtype("int[]|int","int"); }
	@Test public void test_2490() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2491() { checkNotSubtype("int[]|int","int|null"); }
	@Test public void test_2492() { checkIsSubtype("int[]|int","int"); }
	@Test public void test_2494() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2498() { checkNotSubtype("int[]|int","any"); }
	@Test public void test_2499() { checkNotSubtype("int[]|int","null[]|null"); }
	@Test public void test_2500() { checkIsSubtype("int[]|int","int[]|int"); }

	private void checkIsSubtype(String from, String to) {
		Type ft = Type.fromString(from);
		Type tt = Type.fromString(to);
		try {
			assertTrue(new TypeSystem(null).isSubtype(ft,tt));
		} catch(ResolveError e) {
			throw new RuntimeException(e);
		}
	}
	private void checkNotSubtype(String from, String to) {
		Type ft = Type.fromString(from);
		Type tt = Type.fromString(to);
		try {
			assertFalse(new TypeSystem(null).isSubtype(ft, tt));
		} catch (ResolveError e) {
			throw new RuntimeException(e);
		}
	}
}
