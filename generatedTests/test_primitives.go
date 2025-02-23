package main

import (
)

func test_SimpleNonLinearEquation() {

	// Test case 1
	1128@param#a := 2.000000000000001
	1127@param#d := 2
    result := SimpleNonLinearEquation(1128@param#a, 1127@param#d)
	if result != 0 {
	    t.Errorf("test_SimpleNonLinearEquation1 failed")
	}
	
	// Test case 2
	1128@param#a := 1.0
	1127@param#d := 1
    result := SimpleNonLinearEquation(1128@param#a, 1127@param#d)
	if result != 1 {
	    t.Errorf("test_SimpleNonLinearEquation2 failed")
	}
	
}

func test_Mul() {

	// Test case 1
	1131@param#b := NaN
	1130@param#a := -1.0000000000000013
	1129@param#d := 2
    result := Mul(1131@param#b, 1130@param#a, 1129@param#d)
	if result != 3.495959950985713E246 {
	    t.Errorf("test_Mul1 failed")
	}
	
	// Test case 2
	1130@param#a := 1.0
	1131@param#b := 1.0
	1129@param#d := 2
    result := Mul(1130@param#a, 1131@param#b, 1129@param#d)
	if result != 2.860444667617094E-247 {
	    t.Errorf("test_Mul2 failed")
	}
	
	// Test case 3
	1131@param#b := -1.000000000000001
	1130@param#a := -1.0
	1129@param#d := 1
    result := Mul(1131@param#b, 1130@param#a, 1129@param#d)
	if result != -2.6442238751609944E123 {
	    t.Errorf("test_Mul3 failed")
	}
	
	// Test case 4
	1131@param#b := -1.0000000000002274
	1130@param#a := 1.0000000000002276
	1129@param#d := 1
    result := Mul(1131@param#b, 1130@param#a, 1129@param#d)
	if result != 1.0000000000002276 {
	    t.Errorf("test_Mul4 failed")
	}
	
}

func test_SimpleMul() {

	// Test case 1
	1133@param#a := -1.0
	1132@param#d := 1
	1134@param#b := -65536.00000000007
    result := SimpleMul(1133@param#a, 1132@param#d, 1134@param#b)
	if result != -2.6442238751609944E123 {
	    t.Errorf("test_SimpleMul1 failed")
	}
	
	// Test case 2
	1133@param#a := 1.0
	1132@param#d := 1
	1134@param#b := -1.0
    result := SimpleMul(1133@param#a, 1132@param#d, 1134@param#b)
	if result != 3.495959950985713E246 {
	    t.Errorf("test_SimpleMul2 failed")
	}
	
	// Test case 3
	1133@param#a := 1.0000000000002274
	1132@param#d := 1
	1134@param#b := 1.0000000000002276
    result := SimpleMul(1133@param#a, 1132@param#d, 1134@param#b)
	if result != 1.0000000000002276 {
	    t.Errorf("test_SimpleMul3 failed")
	}
	
}

func test_ShortOverflow() {

	// Test case 1
	1135@param#o := 1
	1136@param#x := 65530
	1137@param#y := 3
    result := ShortOverflow(1135@param#o, 1136@param#x, 1137@param#y)
	if result != 65533 {
	    t.Errorf("test_ShortOverflow1 failed")
	}
	
	// Test case 2
	1135@param#o := 1
	1136@param#x := 32767
	1137@param#y := 10
    result := ShortOverflow(1135@param#o, 1136@param#x, 1137@param#y)
	if result != "make interface{} <- string ("IllegalStateExcep...":string)" {
	    t.Errorf("test_ShortOverflow2 failed")
	}
	
	// Test case 3
	1136@param#x := 0
	1135@param#o := 2
	1137@param#y := 11
    result := ShortOverflow(1136@param#x, 1135@param#o, 1137@param#y)
	if result != 0 {
	    t.Errorf("test_ShortOverflow3 failed")
	}
	
	// Test case 4
	1136@param#x := 0
	1135@param#o := 1
	1137@param#y := 10
    result := ShortOverflow(1136@param#x, 1135@param#o, 1137@param#y)
	if result != 10 {
	    t.Errorf("test_ShortOverflow4 failed")
	}
	
	// Test case 5
	1135@param#o := 1
	1136@param#x := 0
	1137@param#y := 0
    result := ShortOverflow(1135@param#o, 1136@param#x, 1137@param#y)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ShortOverflow5 failed")
	}
	
}

func test_Div() {

	// Test case 1
	1138@param#d := 1
	1139@param#a := 1.0
	1141@param#c := 1.0
	1140@param#b := 1.0
    result := Div(1138@param#d, 1139@param#a, 1141@param#c, 1140@param#b)
	if result != 1.0000000000000002 {
	    t.Errorf("test_Div1 failed")
	}
	
}

func test_SimpleSum() {

	// Test case 1
	1143@param#a := 1.0000000000000007
	1144@param#b := 1.0
	1142@param#d := 1
    result := SimpleSum(1143@param#a, 1144@param#b, 1142@param#d)
	if result != -2.6442238751609944E123 {
	    t.Errorf("test_SimpleSum1 failed")
	}
	
	// Test case 2
	1143@param#a := -1.0
	1144@param#b := 1.0
	1142@param#d := 2
    result := SimpleSum(1143@param#a, 1144@param#b, 1142@param#d)
	if result != 3.495959950985713E246 {
	    t.Errorf("test_SimpleSum2 failed")
	}
	
	// Test case 3
	1143@param#a := 1.0000000000002274
	1144@param#b := -1.0000000000002274
	1142@param#d := 1
    result := SimpleSum(1143@param#a, 1144@param#b, 1142@param#d)
	if result != 1.0000000000002276 {
	    t.Errorf("test_SimpleSum3 failed")
	}
	
}

func test_CompareSum() {

	// Test case 1
	1145@param#d := 1
	1146@param#a := 2.0000000000000013
	1147@param#b := -1.0
    result := CompareSum(1145@param#d, 1146@param#a, 1147@param#b)
	if result != 1.0 {
	    t.Errorf("test_CompareSum1 failed")
	}
	
	// Test case 2
	1145@param#d := 1
	1147@param#b := 1.0
	1146@param#a := 1.0
    result := CompareSum(1145@param#d, 1147@param#b, 1146@param#a)
	if result != 1.0000000000002276 {
	    t.Errorf("test_CompareSum2 failed")
	}
	
}

func test_CheckNaN() {

	// Test case 1
	1149@param#dValue := -1.0000000000002276
	1148@param#d := 1
    result := CheckNaN(1149@param#dValue, 1148@param#d)
	if result != 0 {
	    t.Errorf("test_CheckNaN1 failed")
	}
	
	// Test case 2
	1149@param#dValue := 1.0000000000002274
	1148@param#d := 1
    result := CheckNaN(1149@param#dValue, 1148@param#d)
	if result != 100 {
	    t.Errorf("test_CheckNaN2 failed")
	}
	
	// Test case 3
	1148@param#d := 1
	1149@param#dValue := 1.0
    result := CheckNaN(1148@param#d, 1149@param#dValue)
	if result != 1 {
	    t.Errorf("test_CheckNaN3 failed")
	}
	
	// Test case 4
	1148@param#d := 1
	1149@param#dValue := -1.0
    result := CheckNaN(1148@param#d, 1149@param#dValue)
	if result != -1 {
	    t.Errorf("test_CheckNaN4 failed")
	}
	
}

func test_SimpleEquation() {

	// Test case 1
	1150@param#d := 1
	1151@param#a := 1.0000000000000009
    result := SimpleEquation(1150@param#d, 1151@param#a)
	if result != 1 {
	    t.Errorf("test_SimpleEquation1 failed")
	}
	
	// Test case 2
	1150@param#d := 1
	1151@param#a := 2.000000000000001
    result := SimpleEquation(1150@param#d, 1151@param#a)
	if result != 0 {
	    t.Errorf("test_SimpleEquation2 failed")
	}
	
}

func test_UnaryMinus() {

	// Test case 1
	1152@param#d := 1
	1153@param#dValue := -2.000000000000455
    result := UnaryMinus(1152@param#d, 1153@param#dValue)
	if result != 0 {
	    t.Errorf("test_UnaryMinus1 failed")
	}
	
	// Test case 2
	1152@param#d := 2
	1153@param#dValue := 1.0000000000000002
    result := UnaryMinus(1152@param#d, 1153@param#dValue)
	if result != -1 {
	    t.Errorf("test_UnaryMinus2 failed")
	}
	
}

func test_CheckNonInteger() {

	// Test case 1
	1155@param#a := NaN
	1154@param#d := 2
    result := CheckNonInteger(1155@param#a, 1154@param#d)
	if result != 1.0 {
	    t.Errorf("test_CheckNonInteger1 failed")
	}
	
	// Test case 2
	1155@param#a := 2.000000000000455
	1154@param#d := 1
    result := CheckNonInteger(1155@param#a, 1154@param#d)
	if result != 1.0000000000002276 {
	    t.Errorf("test_CheckNonInteger2 failed")
	}
	
}

func test_Compare() {

	// Test case 1
	1157@param#a := 1.0000000000000144
	1156@param#d := 2
	1158@param#b := -1.0
    result := Compare(1157@param#a, 1156@param#d, 1158@param#b)
	if result != 1.0 {
	    t.Errorf("test_Compare1 failed")
	}
	
	// Test case 2
	1156@param#d := 1
	1157@param#a := -1.0
	1158@param#b := -1.0
    result := Compare(1156@param#d, 1157@param#a, 1158@param#b)
	if result != 1.0000000000002276 {
	    t.Errorf("test_Compare2 failed")
	}
	
}

func test_DoubleInfinity() {

	// Test case 1
	1159@param#d := 1
	1160@param#dValue := NaN
    result := DoubleInfinity(1159@param#d, 1160@param#dValue)
	if result != 3 {
	    t.Errorf("test_DoubleInfinity1 failed")
	}
	
	// Test case 2
	1159@param#d := 1
	1160@param#dValue := -1.0000000000002274
    result := DoubleInfinity(1159@param#d, 1160@param#dValue)
	if result != 2 {
	    t.Errorf("test_DoubleInfinity2 failed")
	}
	
	// Test case 3
	1159@param#d := 1
	1160@param#dValue := 1.0000000000002274
    result := DoubleInfinity(1159@param#d, 1160@param#dValue)
	if result != 1 {
	    t.Errorf("test_DoubleInfinity3 failed")
	}
	
}

func test_CompareWithDiv() {

	// Test case 1
	1161@param#d := 1
	1163@param#b := 1.0
	1162@param#a := -8.13966605576524E236
    result := CompareWithDiv(1161@param#d, 1163@param#b, 1162@param#a)
	if result != 1.0000000000002276 {
	    t.Errorf("test_CompareWithDiv1 failed")
	}
	
	// Test case 2
	1161@param#d := 2
	1163@param#b := -4096.000000000005
	1162@param#a := 1.0
    result := CompareWithDiv(1161@param#d, 1163@param#b, 1162@param#a)
	if result != 1.0 {
	    t.Errorf("test_CompareWithDiv2 failed")
	}
	
}

func test_Sum() {

	// Test case 1
	1166@param#b := 6.61055968790249E122
	1165@param#a := -1.0
	1164@param#d := 2
    result := Sum(1166@param#b, 1165@param#a, 1164@param#d)
	if result != -2.6442238751609944E123 {
	    t.Errorf("test_Sum1 failed")
	}
	
	// Test case 2
	1166@param#b := 1.0
	1165@param#a := -1.0
	1164@param#d := 1
    result := Sum(1166@param#b, 1165@param#a, 1164@param#d)
	if result != 3.495959950985713E246 {
	    t.Errorf("test_Sum2 failed")
	}
	
	// Test case 3
	1166@param#b := 1.0000000000002274
	1165@param#a := -1.0000000000002274
	1164@param#d := 1
    result := Sum(1166@param#b, 1165@param#a, 1164@param#d)
	if result != 1.0000000000002276 {
	    t.Errorf("test_Sum3 failed")
	}
	
}

func main() {
	test_SimpleNonLinearEquation()
	test_Mul()
	test_SimpleMul()
	test_ShortOverflow()
	test_Div()
	test_SimpleSum()
	test_CompareSum()
	test_CheckNaN()
	test_SimpleEquation()
	test_UnaryMinus()
	test_CheckNonInteger()
	test_Compare()
	test_DoubleInfinity()
	test_CompareWithDiv()
	test_Sum()
}

// Original code:


type DoubleExamples struct{}

func (d *DoubleExamples) CompareSum(a, b float64) float64 {
	z := a + b
	if z > 5.6 {
		return 1.0
	} else {
		return 0.0
	}
}

func (d *DoubleExamples) Compare(a, b float64) float64 {
	if a > b {
		return 1.0
	} else {
		return 0.0
	}
}

func (d *DoubleExamples) CompareWithDiv(a, b float64) float64 {
	z := a + 0.5
	if (a / z) > b {
		return 1.0
	} else {
		return 0.0
	}
}

func (d *DoubleExamples) SimpleSum(a, b float64) float64 {
	if math.IsNaN(a + b) {
		return 0.0
	}
	c := a + 1.1
	if b+c > 10.1 && b+c < 11.125 {
		return 1.1
	} else {
		return 1.2
	}
}

func (d *DoubleExamples) Sum(a, b float64) float64 {
	if math.IsNaN(a + b) {
		return 0.0
	}
	c := a + 0.123124
	if b+c > 11.123124 && b+c < 11.125 {
		return 1.1
	} else {
		return 1.2
	}
}

func (d *DoubleExamples) SimpleMul(a, b float64) float64 {
	if math.IsNaN(a * b) {
		return 0.0
	}
	if a*b > 33.1 && a*b < 33.875 {
		return 1.1
	} else {
		return 1.2
	}
}

func (d *DoubleExamples) Mul(a, b float64) float64 {
	if math.IsNaN(a * b) {
		return 0.0
	}
	if a*b > 33.32 && a*b < 33.333 {
		return 1.1
	} else if a*b > 33.333 && a*b < 33.7592 {
		return 1.2
	} else {
		return 1.3
	}
}

func (d *DoubleExamples) CheckNonInteger(a float64) float64 {
	if a > 0.1 && a < 0.9 {
		return 1.0
	}
	return 0.0
}

func (d *DoubleExamples) Div(a, b, c float64) float64 {
	return (a + b) / c
}

func (d *DoubleExamples) SimpleEquation(a float64) int {
	if a+a+a-9 == a+3 {
		return 0
	} else {
		return 1
	}
}

func (d *DoubleExamples) SimpleNonLinearEquation(a float64) int {
	if 3*a-9 == a+3 {
		return 0
	} else {
		return 1
	}
}

func (d *DoubleExamples) CheckNaN(dValue float64) int {
	if dValue < 0 {
		return -1
	}
	if dValue > 0 {
		return 1
	}
	if dValue == 0 {
		return 0
	}
	// NaN
	return 100
}

func (d *DoubleExamples) UnaryMinus(dValue float64) int {
	if -dValue < 0 {
		return -1
	}
	return 0
}

func (d *DoubleExamples) DoubleInfinity(dValue float64) int {
	if dValue == math.Inf(1) {
		return 1
	}
	if dValue == math.Inf(-1) {
		return 2
	}
	return 3
}

type Overflow struct{}

func (o *Overflow) ShortOverflow(x int16, y int16) int16 {
	if y > 10 || y <= 0 {
		return 0
	}
	if x+y < 0 && x > 0 {
		panic("IllegalStateException")
	}
	return x + y
}

