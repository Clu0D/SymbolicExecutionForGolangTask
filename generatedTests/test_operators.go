package main

import (
)

func test_And() {

	// Test case 1
	1088@param#value := 0
	1087@param#b := 1
    result := And(1088@param#value, 1087@param#b)
	if result != true {
	    t.Errorf("test_And1 failed")
	}
	
}

func test_BooleanNot() {

	// Test case 1
	1091@param#boolB := true
	1090@param#boolA := true
	1089@param#b := 1
    result := BooleanNot(1091@param#boolB, 1090@param#boolA, 1089@param#b)
	if result != 100 {
	    t.Errorf("test_BooleanNot1 failed")
	}
	
	// Test case 2
	1091@param#boolB := false
	1090@param#boolA := true
	1089@param#b := 1
    result := BooleanNot(1091@param#boolB, 1090@param#boolA, 1089@param#b)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_BooleanNot2 failed")
	}
	
	// Test case 3
	1091@param#boolB := true
	1090@param#boolA := false
	1089@param#b := 1
    result := BooleanNot(1091@param#boolB, 1090@param#boolA, 1089@param#b)
	if result != 200 {
	    t.Errorf("test_BooleanNot3 failed")
	}
	
}

func test_BooleanXor() {

	// Test case 1
	1094@param#valB := false
	1093@param#valA := true
	1092@param#b := 1
    result := BooleanXor(1094@param#valB, 1093@param#valA, 1092@param#b)
	if result != true {
	    t.Errorf("test_BooleanXor1 failed")
	}
	
}

func test_BooleanXorCompare() {

	// Test case 1
	1096@param#aBool := true
	1095@param#b := 1
	1097@param#bBool := true
    result := BooleanXorCompare(1096@param#aBool, 1095@param#b, 1097@param#bBool)
	if result != 0 {
	    t.Errorf("test_BooleanXorCompare1 failed")
	}
	
	// Test case 2
	1096@param#aBool := true
	1095@param#b := 1
	1097@param#bBool := false
    result := BooleanXorCompare(1096@param#aBool, 1095@param#b, 1097@param#bBool)
	if result != 1 {
	    t.Errorf("test_BooleanXorCompare2 failed")
	}
	
}

func test_Shl() {

	// Test case 1
	1099@param#num := 0
	1098@param#b := 1
    result := Shl(1099@param#num, 1098@param#b)
	if result != false {
	    t.Errorf("test_Shl1 failed")
	}
	
}

func test_Shr() {

	// Test case 1
	1100@param#b := 1
	1101@param#num := 0
    result := Shr(1100@param#b, 1101@param#num)
	if result != false {
	    t.Errorf("test_Shr1 failed")
	}
	
}

func test_Complement() {

	// Test case 1
	1102@param#b := 1
	1103@param#x := 0
    result := Complement(1102@param#b, 1103@param#x)
	if result != false {
	    t.Errorf("test_Complement1 failed")
	}
	
}

func test_ShlWithBigLongShift() {

	// Test case 1
	1104@param#b := 1
	1105@param#shift := 48
    result := ShlWithBigLongShift(1104@param#b, 1105@param#shift)
	if result != 3 {
	    t.Errorf("test_ShlWithBigLongShift1 failed")
	}
	
	// Test case 2
	1104@param#b := 1
	1105@param#shift := 0
    result := ShlWithBigLongShift(1104@param#b, 1105@param#shift)
	if result != 1 {
	    t.Errorf("test_ShlWithBigLongShift2 failed")
	}
	
}

func test_BooleanAnd() {

	// Test case 1
	1107@param#aVal := true
	1106@param#b := 1
	1108@param#bVal := true
    result := BooleanAnd(1107@param#aVal, 1106@param#b, 1108@param#bVal)
	if result != true {
	    t.Errorf("test_BooleanAnd1 failed")
	}
	
	// Test case 2
	1107@param#aVal := false
	1106@param#b := 1
	1108@param#bVal := true
    result := BooleanAnd(1107@param#aVal, 1106@param#b, 1108@param#bVal)
	if result != false {
	    t.Errorf("test_BooleanAnd2 failed")
	}
	
}

func test_BooleanOr() {

	// Test case 1
	1110@param#aVal := false
	1111@param#bVal := true
	1109@param#b := 1
    result := BooleanOr(1110@param#aVal, 1111@param#bVal, 1109@param#b)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_BooleanOr1 failed")
	}
	
}

func test_ShlLong() {

	// Test case 1
	1112@param#b := 1
	1113@param#longNum := 0
    result := ShlLong(1112@param#b, 1113@param#longNum)
	if result != false {
	    t.Errorf("test_ShlLong1 failed")
	}
	
}

func test_ShrLong() {

	// Test case 1
	1114@param#b := 1
	1115@param#longVal := 0
    result := ShrLong(1114@param#b, 1115@param#longVal)
	if result != false {
	    t.Errorf("test_ShrLong1 failed")
	}
	
}

func test_Or() {

	// Test case 1
	1116@param#b := 1
	1117@param#val := 0
    result := Or(1116@param#b, 1117@param#val)
	if result != false {
	    t.Errorf("test_Or1 failed")
	}
	
}

func test_Sign() {

	// Test case 1
	1119@param#num := 1
	1118@param#b := 1
    result := Sign(1119@param#num, 1118@param#b)
	if result != -1 {
	    t.Errorf("test_Sign1 failed")
	}
	
	// Test case 2
	1119@param#num := 0
	1118@param#b := 1
    result := Sign(1119@param#num, 1118@param#b)
	if result != 0 {
	    t.Errorf("test_Sign2 failed")
	}
	
}

func test_Ushr() {

	// Test case 1
	1121@param#unsignedVal := 0
	1120@param#b := 1
    result := Ushr(1121@param#unsignedVal, 1120@param#b)
	if result != false {
	    t.Errorf("test_Ushr1 failed")
	}
	
}

func test_UshrLong() {

	// Test case 1
	1122@param#b := 1
	1123@param#unsignedLongVal := 0
    result := UshrLong(1122@param#b, 1123@param#unsignedLongVal)
	if result != false {
	    t.Errorf("test_UshrLong1 failed")
	}
	
}

func test_Xor() {

	// Test case 1
	1124@param#b := 1
	1126@param#y := -1
	1125@param#x := 0
    result := Xor(1124@param#b, 1126@param#y, 1125@param#x)
	if result != false {
	    t.Errorf("test_Xor1 failed")
	}
	
}

func main() {
	test_And()
	test_BooleanNot()
	test_BooleanXor()
	test_BooleanXorCompare()
	test_Shl()
	test_Shr()
	test_Complement()
	test_ShlWithBigLongShift()
	test_BooleanAnd()
	test_BooleanOr()
	test_ShlLong()
	test_ShrLong()
	test_Or()
	test_Sign()
	test_Ushr()
	test_UshrLong()
	test_Xor()
}

// Original code:

type BitOperators struct{}

func (b *BitOperators) Complement(x int) bool {
	return ^x == 1
}

func (b *BitOperators) Xor(x, y int) bool {
	return (x ^ y) == 0
}

func (b *BitOperators) Or(val int) bool {
	return (val | 7) == 15
}

func (b *BitOperators) And(value int) bool {
	return (value & (value - 1)) == 0
}

func (b *BitOperators) BooleanNot(boolA, boolB bool) int {
	d := boolA && boolB
	e := (!boolA) || boolB // Используем корректные имена переменных
	if d && e {
		return 100
	}
	return 200
}

func (b *BitOperators) BooleanXor(valA, valB bool) bool {
	return valA != valB
}

func (b *BitOperators) BooleanOr(aVal, bVal bool) bool {
	return aVal || bVal
}

func (b *BitOperators) BooleanAnd(aVal, bVal bool) bool {
	return aVal && bVal
}

func (b *BitOperators) BooleanXorCompare(aBool, bBool bool) int {
	if aBool != bBool {
		return 1
	}
	return 0
}

func (b *BitOperators) Shl(num int) bool {
	return (num << 1) == 2
}

func (b *BitOperators) ShlLong(longNum int64) bool {
	return (longNum << 1) == 2
}

func (b *BitOperators) ShlWithBigLongShift(shift int64) int {
	if shift < 40 {
		return 1
	}
	if (0x77777777 << shift) == 0x77777770 {
		return 2
	}
	return 3
}

func (b *BitOperators) Shr(num int) bool {
	return (num >> 1) == 1
}

func (b *BitOperators) ShrLong(longVal int64) bool {
	return (longVal >> 1) == 1
}

func (b *BitOperators) Ushr(unsignedVal int) bool {
	return (unsignedVal >> 1) == 1 // В Go сдвиг вправо всегда беззнаковый
}

func (b *BitOperators) UshrLong(unsignedLongVal int64) bool {
	return (unsignedLongVal >> 1) == 1 // В Go сдвиг вправо всегда беззнаковый
}

func (b *BitOperators) Sign(num int) int {
	i := (num >> 31) | (-num >> 31)

	if i > 0 {
		return 1
	} else if i == 0 {
		return 0
	} else {
		return -1
	}
}

