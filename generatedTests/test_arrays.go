package main

import (
)

func test_BooleanArray() {

	// Test case 1
	3@param#arr := 1
	1@param#arr:len := 0
	0@param#a := 2
    result := BooleanArray(3@param#arr, 1@param#arr:len, 0@param#a)
	if result != 3 {
	    t.Errorf("test_BooleanArray1 failed")
	}
	
	// Test case 2
	3@param#arr := 1
	1@param#arr:len := 4
	0@param#a := 1
    result := BooleanArray(3@param#arr, 1@param#arr:len, 0@param#a)
	if result != 2 {
	    t.Errorf("test_BooleanArray2 failed")
	}
	
	// Test case 3
	3@param#arr := 1
	1@param#arr:len := 5
	0@param#a := 2
    result := BooleanArray(3@param#arr, 1@param#arr:len, 0@param#a)
	if result != 1 {
	    t.Errorf("test_BooleanArray3 failed")
	}
	
}

func test_CreateNewThreeDimensionalArray() {

	// Test case 1
	9@param#length := 2
	10@param#constValue := 64
	8@param#a := 1
    result := CreateNewThreeDimensionalArray(9@param#length, 10@param#constValue, 8@param#a)
	if result != G*[[[INT64]]] {
	    t.Errorf("test_CreateNewThreeDimensionalArray1 failed")
	}
	
	// Test case 2
	9@param#length := 137438953474
	8@param#a := 2
	10@param#constValue := 0
    result := CreateNewThreeDimensionalArray(9@param#length, 8@param#a, 10@param#constValue)
	if result != G*[[[INT64]]] {
	    t.Errorf("test_CreateNewThreeDimensionalArray2 failed")
	}
	
}

func test_ReallyMultiDimensionalArray() {

	// Test case 1
	24@param#array := 1
	20@param#a := 2
	21@param#array:len := 0
    result := ReallyMultiDimensionalArray(24@param#array, 20@param#a, 21@param#array:len)
	if result != G*[[[INT64]]] {
	    t.Errorf("test_ReallyMultiDimensionalArray1 failed")
	}
	
	// Test case 2
	24@param#array := 1
	20@param#a := 1
	21@param#array:len := 0
    result := ReallyMultiDimensionalArray(24@param#array, 20@param#a, 21@param#array:len)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ReallyMultiDimensionalArray2 failed")
	}
	
}

func test_ShortSizeAndIndex() {

	// Test case 1
	40@param#p := 1
	41@param#a:len := 2
	44@param#x := 2
	43@param#a := 1
    result := ShortSizeAndIndex(40@param#p, 41@param#a:len, 44@param#x, 43@param#a)
	if result != 0 {
	    t.Errorf("test_ShortSizeAndIndex1 failed")
	}
	
	// Test case 2
	40@param#p := 1
	41@param#a:len := 4
	44@param#x := 16
	43@param#a := 1
    result := ShortSizeAndIndex(40@param#p, 41@param#a:len, 44@param#x, 43@param#a)
	if result != 1 {
	    t.Errorf("test_ShortSizeAndIndex2 failed")
	}
	
	// Test case 3
	40@param#p := 2
	41@param#a:len := 0
	44@param#x := 164
	43@param#a := 2
    result := ShortSizeAndIndex(40@param#p, 41@param#a:len, 44@param#x, 43@param#a)
	if result != 255 {
	    t.Errorf("test_ShortSizeAndIndex3 failed")
	}
	
	// Test case 4
	40@param#p := 2
	41@param#a:len := 0
	44@param#x := 32768
	43@param#a := 2
    result := ShortSizeAndIndex(40@param#p, 41@param#a:len, 44@param#x, 43@param#a)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ShortSizeAndIndex4 failed")
	}
	
}

func test_ByteSizeAndIndex() {

	// Test case 1
	53@param#x := 64
	49@param#p := 2
	50@param#a:len := 1
	52@param#a := 1
    result := ByteSizeAndIndex(53@param#x, 49@param#p, 50@param#a:len, 52@param#a)
	if result != 1 {
	    t.Errorf("test_ByteSizeAndIndex1 failed")
	}
	
	// Test case 2
	53@param#x := 2
	49@param#p := 1
	50@param#a:len := 1
	52@param#a := 1
    result := ByteSizeAndIndex(53@param#x, 49@param#p, 50@param#a:len, 52@param#a)
	if result != 0 {
	    t.Errorf("test_ByteSizeAndIndex2 failed")
	}
	
	// Test case 3
	53@param#x := 0
	49@param#p := 2
	50@param#a:len := 0
	52@param#a := 2
    result := ByteSizeAndIndex(53@param#x, 49@param#p, 50@param#a:len, 52@param#a)
	if result != 255 {
	    t.Errorf("test_ByteSizeAndIndex3 failed")
	}
	
	// Test case 4
	53@param#x := 0
	49@param#p := 1
	50@param#a:len := 0
	52@param#a := 1
    result := ByteSizeAndIndex(53@param#x, 49@param#p, 50@param#a:len, 52@param#a)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ByteSizeAndIndex4 failed")
	}
	
}

func test_CharArray() {

	// Test case 1
	61@param#arr := 1
	59@param#arr:len := 0
	58@param#a := 1
    result := CharArray(61@param#arr, 59@param#arr:len, 58@param#a)
	if result != 3 {
	    t.Errorf("test_CharArray1 failed")
	}
	
	// Test case 2
	61@param#arr := 1
	59@param#arr:len := 0
	58@param#a := 1
    result := CharArray(61@param#arr, 59@param#arr:len, 58@param#a)
	if result != 2 {
	    t.Errorf("test_CharArray2 failed")
	}
	
	// Test case 3
	61@param#arr := 1
	59@param#arr:len := 0
	58@param#a := 1
    result := CharArray(61@param#arr, 59@param#arr:len, 58@param#a)
	if result != 1 {
	    t.Errorf("test_CharArray3 failed")
	}
	
}

func test_CharSizeAndIndex() {

	// Test case 1
	66@param#p := 2
	70@param#x := 2
	69@param#a := 1
	67@param#a:len := 0
    result := CharSizeAndIndex(66@param#p, 70@param#x, 69@param#a, 67@param#a:len)
	if result != 0 {
	    t.Errorf("test_CharSizeAndIndex1 failed")
	}
	
	// Test case 2
	66@param#p := 1
	70@param#x := 17
	69@param#a := 1
	67@param#a:len := 0
    result := CharSizeAndIndex(66@param#p, 70@param#x, 69@param#a, 67@param#a:len)
	if result != 1 {
	    t.Errorf("test_CharSizeAndIndex2 failed")
	}
	
	// Test case 3
	66@param#p := 2
	70@param#x := 2147483631
	69@param#a := 2
	67@param#a:len := 0
    result := CharSizeAndIndex(66@param#p, 70@param#x, 69@param#a, 67@param#a:len)
	if result != 255 {
	    t.Errorf("test_CharSizeAndIndex3 failed")
	}
	
	// Test case 4
	66@param#p := 2
	70@param#x := 0
	69@param#a := 2
	67@param#a:len := 0
    result := CharSizeAndIndex(66@param#p, 70@param#x, 69@param#a, 67@param#a:len)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_CharSizeAndIndex4 failed")
	}
	
}

func test_DefaultBooleanValues() {

	// Test case 1
	75@param#p := 1
    result := DefaultBooleanValues(75@param#p)
	if result != G*[BOOL] {
	    t.Errorf("test_DefaultBooleanValues1 failed")
	}
	
}

func test_DefaultDoubleValues() {

	// Test case 1
	79@param#p := 1
    result := DefaultDoubleValues(79@param#p)
	if result != G*[FLOAT64] {
	    t.Errorf("test_DefaultDoubleValues1 failed")
	}
	
}

func test_DoubleArray() {

	// Test case 1
	86@param#arr := 1
	83@param#a := 2
    result := DoubleArray(86@param#arr, 83@param#a)
	if result != 3 {
	    t.Errorf("test_DoubleArray1 failed")
	}
	
	// Test case 2
	86@param#arr := 2
	84@param#arr:len := 1
	83@param#a := 2
    result := DoubleArray(86@param#arr, 84@param#arr:len, 83@param#a)
	if result != 2 {
	    t.Errorf("test_DoubleArray2 failed")
	}
	
	// Test case 3
	86@param#arr := 1
	84@param#arr:len := 0
	83@param#a := 2
    result := DoubleArray(86@param#arr, 84@param#arr:len, 83@param#a)
	if result != 1 {
	    t.Errorf("test_DoubleArray3 failed")
	}
	
}

func test_CopyArray() {

	// Test case 1
	92@param#a := 1
	94@param#array := 1
    result := CopyArray(92@param#a, 94@param#array)
	if result != G*[*NAMED:ObjectWithPrimitivesClass], string!val!96784904 {
	    t.Errorf("test_CopyArray1 failed")
	}
	
	// Test case 2
	92@param#a := 1
	94@param#array := 1
	93@param#array:len := -9223372036854775808
    result := CopyArray(92@param#a, 94@param#array, 93@param#array:len)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_CopyArray2 failed")
	}
	
}

func test_DefaultValues() {

	// Test case 1
	132@param#a := 1
    result := DefaultValues(132@param#a)
	if result != G*[[[INT64]]] {
	    t.Errorf("test_DefaultValues1 failed")
	}
	
}

func test_ObjectArray() {

	// Test case 1
	139@param#array := 1
	144@param#objectExample := 1
	137@param#a := 1
    result := ObjectArray(139@param#array, 144@param#objectExample, 137@param#a)
	if result != 1 {
	    t.Errorf("test_ObjectArray1 failed")
	}
	
	// Test case 2
	139@param#array := 1
	144@param#objectExample := 1
	137@param#a := 1
    result := ObjectArray(139@param#array, 144@param#objectExample, 137@param#a)
	if result != 0 {
	    t.Errorf("test_ObjectArray2 failed")
	}
	
	// Test case 3
	139@param#array := 1
	144@param#objectExample := 2
	138@param#array:len := 2
	137@param#a := 1
    result := ObjectArray(139@param#array, 144@param#objectExample, 138@param#array:len, 137@param#a)
	if result != -1 {
	    t.Errorf("test_ObjectArray3 failed")
	}
	
}

func test_ShortArray() {

	// Test case 1
	160@param#arr := 2
	157@param#a := 1
	158@param#arr:len := 0
    result := ShortArray(160@param#arr, 157@param#a, 158@param#arr:len)
	if result != 3 {
	    t.Errorf("test_ShortArray1 failed")
	}
	
	// Test case 2
	160@param#arr := 2
	157@param#a := 1
	158@param#arr:len := 3
    result := ShortArray(160@param#arr, 157@param#a, 158@param#arr:len)
	if result != 2 {
	    t.Errorf("test_ShortArray2 failed")
	}
	
	// Test case 3
	160@param#arr := 1
	157@param#a := 1
	158@param#arr:len := -468
    result := ShortArray(160@param#arr, 157@param#a, 158@param#arr:len)
	if result != 1 {
	    t.Errorf("test_ShortArray3 failed")
	}
	
}

func test_CreateNewMultiDimensionalArray() {

	// Test case 1
	167@param#j := -9223372036854775808
	166@param#i := 0
	165@param#a := 2
    result := CreateNewMultiDimensionalArray(167@param#j, 166@param#i, 165@param#a)
	if result != G*[[[[INT64]]]] {
	    t.Errorf("test_CreateNewMultiDimensionalArray1 failed")
	}
	
	// Test case 2
	167@param#j := 8
	166@param#i := 2308889258210976143
	165@param#a := 2
    result := CreateNewMultiDimensionalArray(167@param#j, 166@param#i, 165@param#a)
	if result != "out of bounds" {
	    t.Errorf("test_CreateNewMultiDimensionalArray2 failed")
	}
	
	// Test case 3
	167@param#j := 0
	166@param#i := 1
	165@param#a := 1
    result := CreateNewMultiDimensionalArray(167@param#j, 166@param#i, 165@param#a)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_CreateNewMultiDimensionalArray3 failed")
	}
	
}

func test_LongArray() {

	// Test case 1
	180@param#x := 2045
	177@param#a:len := -9223372036854775808
	176@param#p := 2
	179@param#a := 1
    result := LongArray(180@param#x, 177@param#a:len, 176@param#p, 179@param#a)
	if result != 1 {
	    t.Errorf("test_LongArray1 failed")
	}
	
	// Test case 2
	180@param#x := 8
	177@param#a:len := 0
	176@param#p := 2
	179@param#a := 1
    result := LongArray(180@param#x, 177@param#a:len, 176@param#p, 179@param#a)
	if result != 0 {
	    t.Errorf("test_LongArray2 failed")
	}
	
	// Test case 3
	180@param#x := -9223372036854775808
	177@param#a:len := 39
	176@param#p := 1
	179@param#a := 2
    result := LongArray(180@param#x, 177@param#a:len, 176@param#p, 179@param#a)
	if result != 255 {
	    t.Errorf("test_LongArray3 failed")
	}
	
}

func test_NewObjectWithPrimitivesClassWithParams() {

	// Test case 1
	187@param#x := 2
	188@param#y := 0
	189@param#weight := 1.0
    result := NewObjectWithPrimitivesClassWithParams(187@param#x, 188@param#y, 189@param#weight)
	if result != G*NAMED:ObjectWithPrimitivesClass {
	    t.Errorf("test_NewObjectWithPrimitivesClassWithParams1 failed")
	}
	
}

func test_DefaultIntValues() {

	// Test case 1
	194@param#p := 2
    result := DefaultIntValues(194@param#p)
	if result != G*[INT64] {
	    t.Errorf("test_DefaultIntValues1 failed")
	}
	
}

func test_FloatArray() {

	// Test case 1
	198@param#a := 1
	201@param#arr := 1
    result := FloatArray(198@param#a, 201@param#arr)
	if result != 3 {
	    t.Errorf("test_FloatArray1 failed")
	}
	
	// Test case 2
	199@param#arr:len := 2
	198@param#a := 1
	201@param#arr := 1
    result := FloatArray(199@param#arr:len, 198@param#a, 201@param#arr)
	if result != 2 {
	    t.Errorf("test_FloatArray2 failed")
	}
	
	// Test case 3
	199@param#arr:len := 4
	198@param#a := 1
	201@param#arr := 2
    result := FloatArray(199@param#arr:len, 198@param#a, 201@param#arr)
	if result != 1 {
	    t.Errorf("test_FloatArray3 failed")
	}
	
}

func test_DefaultValuesWithoutTwoDimensions() {

	// Test case 1
	207@param#a := 2
	208@param#i := 2
    result := DefaultValuesWithoutTwoDimensions(207@param#a, 208@param#i)
	if result != G*[[[[INT64]]]] {
	    t.Errorf("test_DefaultValuesWithoutTwoDimensions1 failed")
	}
	
	// Test case 2
	208@param#i := 1
	207@param#a := 1
    result := DefaultValuesWithoutTwoDimensions(208@param#i, 207@param#a)
	if result != nil {
	    t.Errorf("test_DefaultValuesWithoutTwoDimensions2 failed")
	}
	
}

func test_CreateArray() {

	// Test case 1
	490@param#length := 3
	487@param#a := 1
    result := CreateArray(490@param#length, 487@param#a)
	if result != G*[*NAMED:ObjectWithPrimitivesClass], string!val!96784904 {
	    t.Errorf("test_CreateArray1 failed")
	}
	
	// Test case 2
	490@param#length := -35182224605184
	487@param#a := 2
	489@param#y := -22820
	488@param#x := 0
    result := CreateArray(490@param#length, 487@param#a, 489@param#y, 488@param#x)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_CreateArray2 failed")
	}
	
}

func test_ByteArray() {

	// Test case 1
	506@param#a := 1
	507@param#arr:len := 0
	509@param#arr := 1
    result := ByteArray(506@param#a, 507@param#arr:len, 509@param#arr)
	if result != 3 {
	    t.Errorf("test_ByteArray1 failed")
	}
	
	// Test case 2
	506@param#a := 1
	507@param#arr:len := 0
	509@param#arr := 1
    result := ByteArray(506@param#a, 507@param#arr:len, 509@param#arr)
	if result != 2 {
	    t.Errorf("test_ByteArray2 failed")
	}
	
	// Test case 3
	506@param#a := 2
	507@param#arr:len := 2
	509@param#arr := 2
    result := ByteArray(506@param#a, 507@param#arr:len, 509@param#arr)
	if result != 1 {
	    t.Errorf("test_ByteArray3 failed")
	}
	
}

func test_IntArray() {

	// Test case 1
	514@param#a := 1
	515@param#arr:len := 0
	517@param#arr := 2
    result := IntArray(514@param#a, 515@param#arr:len, 517@param#arr)
	if result != 3 {
	    t.Errorf("test_IntArray1 failed")
	}
	
	// Test case 2
	514@param#a := 1
	515@param#arr:len := -3
	517@param#arr := 1
    result := IntArray(514@param#a, 515@param#arr:len, 517@param#arr)
	if result != 2 {
	    t.Errorf("test_IntArray2 failed")
	}
	
	// Test case 3
	514@param#a := 2
	515@param#arr:len := 0
	517@param#arr := 1
    result := IntArray(514@param#a, 515@param#arr:len, 517@param#arr)
	if result != 1 {
	    t.Errorf("test_IntArray3 failed")
	}
	
}

func test_DefaultValuesWithoutLastDimension() {

	// Test case 1
	522@param#a := 1
    result := DefaultValuesWithoutLastDimension(522@param#a)
	if result != "out of bounds" {
	    t.Errorf("test_DefaultValuesWithoutLastDimension1 failed")
	}
	
}

func test_FillMultiArrayWithArray() {

	// Test case 1
	529@param#a := 2
	532@param#value := 2
	530@param#value:len := 0
    result := FillMultiArrayWithArray(529@param#a, 532@param#value, 530@param#value:len)
	if result != G*[[INT64]] {
	    t.Errorf("test_FillMultiArrayWithArray1 failed")
	}
	
	// Test case 2
	529@param#a := 2
	532@param#value := 1
	530@param#value:len := 9223372036854775806
    result := FillMultiArrayWithArray(529@param#a, 532@param#value, 530@param#value:len)
	if result != G*[[INT64]] {
	    t.Errorf("test_FillMultiArrayWithArray2 failed")
	}
	
}

func test_SizesWithoutTouchingTheElements() {

	// Test case 1
	547@param#a := 1
    result := SizesWithoutTouchingTheElements(547@param#a)
	if result != G*[[INT64]] {
	    t.Errorf("test_SizesWithoutTouchingTheElements1 failed")
	}
	
}

func test_DefaultValuesNewMultiArray() {

	// Test case 1
	553@param#a := 2
    result := DefaultValuesNewMultiArray(553@param#a)
	if result != G*[[[INT64]]] {
	    t.Errorf("test_DefaultValuesNewMultiArray1 failed")
	}
	
}

func test_IsIdentityMatrix() {

	// Test case 1
	560@param#a := 2
	564@param#matrix := 1
	561@param#matrix:len := 475
    result := IsIdentityMatrix(560@param#a, 564@param#matrix, 561@param#matrix:len)
	if result != false {
	    t.Errorf("test_IsIdentityMatrix1 failed")
	}
	
	// Test case 2
	560@param#a := 1
	564@param#matrix := 1
	561@param#matrix:len := 1
    result := IsIdentityMatrix(560@param#a, 564@param#matrix, 561@param#matrix:len)
	if result != false {
	    t.Errorf("test_IsIdentityMatrix2 failed")
	}
	
	// Test case 3
	560@param#a := 1
	564@param#matrix := 2
	561@param#matrix:len := -44009
    result := IsIdentityMatrix(560@param#a, 564@param#matrix, 561@param#matrix:len)
	if result != false {
	    t.Errorf("test_IsIdentityMatrix3 failed")
	}
	
	// Test case 4
	560@param#a := 1
	564@param#matrix := 1
	561@param#matrix:len := 0
    result := IsIdentityMatrix(560@param#a, 564@param#matrix, 561@param#matrix:len)
	if result != "make interface{} <- string ("IllegalArgumentEx...":string)" {
	    t.Errorf("test_IsIdentityMatrix4 failed")
	}
	
}

func test_MultiDimensionalObjectsArray() {

	// Test case 1
	576@param#array:len := -9223372036854775808
	579@param#array := 2
	575@param#a := 2
    result := MultiDimensionalObjectsArray(576@param#array:len, 579@param#array, 575@param#a)
	if result != G*[[NAMED:Point]] {
	    t.Errorf("test_MultiDimensionalObjectsArray1 failed")
	}
	
}

func main() {
	test_BooleanArray()
	test_CreateNewThreeDimensionalArray()
	test_ReallyMultiDimensionalArray()
	test_ShortSizeAndIndex()
	test_ByteSizeAndIndex()
	test_CharArray()
	test_CharSizeAndIndex()
	test_DefaultBooleanValues()
	test_DefaultDoubleValues()
	test_DoubleArray()
	test_CopyArray()
	test_DefaultValues()
	test_ObjectArray()
	test_ShortArray()
	test_CreateNewMultiDimensionalArray()
	test_LongArray()
	test_NewObjectWithPrimitivesClassWithParams()
	test_DefaultIntValues()
	test_FloatArray()
	test_DefaultValuesWithoutTwoDimensions()
	test_CreateArray()
	test_ByteArray()
	test_IntArray()
	test_DefaultValuesWithoutLastDimension()
	test_FillMultiArrayWithArray()
	test_SizesWithoutTouchingTheElements()
	test_DefaultValuesNewMultiArray()
	test_IsIdentityMatrix()
	test_MultiDimensionalObjectsArray()
}

// Original code:

type Point struct{}

type ColoredPoint struct {
	Point
}

type ArrayOfArrays struct{}

func (a *ArrayOfArrays) DefaultValues() [][][]int {
	array := make([][][]int, 1)
	if array[0] == nil {
		return array
	}
	return array
}

func (a *ArrayOfArrays) SizesWithoutTouchingTheElements() [][]int {
	array := make([][]int, 10)
	for i := range array {
		array[i] = make([]int, 3)
	}
	for i := 0; i < 2; i++ {
		_ = array[i][0]
	}
	return array
}

func (a *ArrayOfArrays) DefaultValuesWithoutLastDimension() [][][][]int {
	array := make([][][][]int, 4)
	for i := range array {
		array[i] = make([][][]int, 4)
		for j := range array[i] {
			array[i][j] = make([][]int, 4)
		}
	}
	if array[0][0][0] != nil {
		return array
	}
	return array
}

func (a *ArrayOfArrays) CreateNewMultiDimensionalArray(i, j int) [][][][]int {
	array := make([][][][]int, 2)
	for idx := range array {
		array[idx] = make([][][]int, i)
		for jdx := range array[idx] {
			array[idx][jdx] = make([][]int, j)
			for kdx := range array[idx][jdx] {
				array[idx][jdx][kdx] = make([]int, 3)
			}
		}
	}
	return array
}

func (a *ArrayOfArrays) DefaultValuesWithoutTwoDimensions(i int) [][][][]int {
	if i < 2 {
		return nil
	}
	array := make([][][][]int, i)
	for idx := range array {
		array[idx] = make([][][]int, i)
	}
	if array[0][0] != nil {
		return array
	}
	return array
}

func (a *ArrayOfArrays) DefaultValuesNewMultiArray() [][][]int {
	array := make([][][]int, 1)
	array[0] = make([][]int, 1)
	array[0][0] = make([]int, 1)
	if array[0] == nil {
		return array
	}
	return array
}

func (a *ArrayOfArrays) IsIdentityMatrix(matrix [][]int) bool {
	if len(matrix) < 3 {
		panic("IllegalArgumentException: matrix length < 3")
	}
	for i := 0; i < len(matrix); i++ {
		if len(matrix[i]) != len(matrix) {
			return false
		}
		for j := 0; j < len(matrix[i]); j++ {
			if i == j && matrix[i][j] != 1 {
				return false
			}
			if i != j && matrix[i][j] != 0 {
				return false
			}
		}
	}
	return true
}

func (a *ArrayOfArrays) CreateNewThreeDimensionalArray(length, constValue int) [][][]int {
	if length != 2 {
		return make([][][]int, 0)
	}
	matrix := make([][][]int, length)
	for i := range matrix {
		matrix[i] = make([][]int, length)
		for j := range matrix[i] {
			matrix[i][j] = make([]int, length)
			for k := range matrix[i][j] {
				matrix[i][j][k] = constValue + 1
			}
		}
	}
	return matrix
}

func (a *ArrayOfArrays) ReallyMultiDimensionalArray(array [][][]int) [][][]int {
	if array[1][2][3] != 12345 {
		array[1][2][3] = 12345
	} else {
		array[1][2][3] -= 12345 * 2
	}
	return array
}

func (a *ArrayOfArrays) MultiDimensionalObjectsArray(array [][]Point) [][]Point {
	array[0] = make([]Point, 2)
	array[1] = make([]Point, 1)
	return array
}

func (a *ArrayOfArrays) FillMultiArrayWithArray(value []int) [][]int {
	if len(value) < 2 {
		return make([][]int, 0)
	}
	for i := range value {
		value[i] += i
	}
	length := 3
	array := make([][]int, length)
	for i := 0; i < length; i++ {
		array[i] = value
	}
	return array
}

import "errors"

type ObjectWithPrimitivesClass struct {
	ValueByDefault int
	x, y           int
	Weight         float64
}

func NewObjectWithPrimitivesClass() *ObjectWithPrimitivesClass {
	return &ObjectWithPrimitivesClass{ValueByDefault: 5}
}

func NewObjectWithPrimitivesClassWithParams(x, y int, weight float64) *ObjectWithPrimitivesClass {
	return &ObjectWithPrimitivesClass{
		ValueByDefault: 5,
		x:              x,
		y:              y,
		Weight:         weight,
	}
}

type ArrayOfObjects struct{}

func (a *ArrayOfObjects) DefaultValues() []*ObjectWithPrimitivesClass {
	array := make([]*ObjectWithPrimitivesClass, 1)
	if array[0] != nil {
		return array
	}
	return array
}

func (a *ArrayOfObjects) CreateArray(x, y, length int) ([]*ObjectWithPrimitivesClass, error) {
	if length < 3 {
		return nil, errors.New("IllegalArgumentException: length must be at least 3")
	}

	array := make([]*ObjectWithPrimitivesClass, length)

	for i := 0; i < len(array); i++ {
		array[i] = NewObjectWithPrimitivesClass()
		array[i].x = x + i
		array[i].y = y + i
	}

	return array, nil
}

func (a *ArrayOfObjects) CopyArray(array []*ObjectWithPrimitivesClass) ([]*ObjectWithPrimitivesClass, error) {
	if len(array) < 3 {
		return nil, errors.New("IllegalArgumentException: array length must be at least 3")
	}

	copy := make([]*ObjectWithPrimitivesClass, len(array))
	for i := 0; i < len(array); i++ {
		copy[i] = array[i]
	}

	for i := 0; i < len(array); i++ {
		array[i].x = -1
		array[i].y = 1
	}

	return copy, nil
}

func (a *ArrayOfObjects) ObjectArray(array []*ObjectWithPrimitivesClass, objectExample *ObjectWithPrimitivesClass) int {
	if len(array) != 2 {
		return -1
	}
	array[0] = NewObjectWithPrimitivesClass()
	array[0].x = 5
	array[1] = objectExample
	if array[0].x+array[1].x > 20 {
		return 1
	}
	return 0
}

type ObjectWithPrimitivesClassSucc struct {
	ObjectWithPrimitivesClass
	AnotherX int
}

type ArraysOverwriteValue struct{}

func (a *ArraysOverwriteValue) ByteArray(arr []byte) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] != 0 {
		return 2
	}
	arr[0] = 1
	return 3
}

func (a *ArraysOverwriteValue) ShortArray(arr []int16) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] != 0 {
		return 2
	}
	arr[0] = 1
	return 3
}

func (a *ArraysOverwriteValue) CharArray(arr []rune) rune {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] != 0 {
		return 2
	}
	arr[0] = 1
	return 3
}

func (a *ArraysOverwriteValue) IntArray(arr []int) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] != 0 {
		return 2
	}
	arr[0] = 1
	return 3
}

func (a *ArraysOverwriteValue) LongArray(arr []int64) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] != 0 {
		return 2
	}
	arr[0] = 1
	return 3
}

func (a *ArraysOverwriteValue) FloatArray(arr []float32) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] == arr[0] {
		return 2
	}
	arr[0] = 1.0
	return 3
}

func (a *ArraysOverwriteValue) DoubleArray(arr []float64) byte {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] == arr[0] {
		return 2
	}
	arr[0] = 1.0
	return 3
}

func (a *ArraysOverwriteValue) BooleanArray(arr []bool) int {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] {
		return 2
	}
	arr[0] = true
	return 3
}

func (a *ArraysOverwriteValue) ObjectArray(arr []*ObjectWithPrimitivesClass) int {
	if len(arr) == 0 {
		return 1
	}
	if arr[0] == nil {
		return 2
	}
	arr[0] = nil
	return 3
}

type PrimitiveArrays struct{}

func (p *PrimitiveArrays) DefaultIntValues() []int {
	array := make([]int, 3)
	if array[1] != 0 {
		return array
	}
	return array
}

func (p *PrimitiveArrays) DefaultDoubleValues() []float64 {
	array := make([]float64, 3)
	if array[1] != 0.0 {
		return array
	}
	return array
}

func (p *PrimitiveArrays) DefaultBooleanValues() []bool {
	array := make([]bool, 3)
	if !array[1] {
		return array
	}
	return array
}

func (p *PrimitiveArrays) ByteArray(a []byte, x byte) byte {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) ShortArray(a []int16, x int16) byte {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) CharArray(a []rune, x rune) byte {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x

	sum := int(a[0]) + int(a[1])
	if sum > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) IntArray(a []int, x int) byte {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) LongArray(a []int64, x int64) int {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) FloatArray(a []float32, x float32) int {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) DoubleArray(a []float64, x float64) int {
	if len(a) != 2 {
		return 255
	}
	a[0] = 5
	a[1] = x
	if a[0]+a[1] > 20 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) BooleanArray(a []bool, x, y bool) int {
	if len(a) != 2 {
		return 255
	}
	a[0] = x
	a[1] = y
	if a[0] != a[1] {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) ByteSizeAndIndex(a []byte, x byte) byte {
	if a == nil || len(a) <= int(x) || x < 1 {
		return 255
	}
	b := make([]byte, x)
	b[0] = 5
	a[x] = x
	if b[0]+a[x] > 7 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) ShortSizeAndIndex(a []int16, x int16) byte {
	if a == nil || len(a) <= int(x) || x < 1 {
		return 255
	}
	b := make([]int16, x)
	b[0] = 5
	a[x] = x
	if b[0]+a[x] > 7 {
		return 1
	}
	return 0
}

func (p *PrimitiveArrays) CharSizeAndIndex(a []rune, x rune) byte {
	if a == nil || len(a) <= int(x) || x < 1 {
		return 255
	}
	b := make([]rune, x)
	b[0] = 5
	a[x] = x
	if b[0]+a[x] > 7 {
		return 1
	}
	return 0
}

