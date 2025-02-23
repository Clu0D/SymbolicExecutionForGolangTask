package main

import (
)

func test_changeArrayValues() {

	// Test case 1
	693@param#array := 1
	694@param#diff := -5493
	690@param#i := 1
	691@param#array:len := 0
    result := changeArrayValues(693@param#array, 694@param#diff, 690@param#i, 691@param#array:len)
	if result !=  {
	    t.Errorf("test_changeArrayValues1 failed")
	}
	
	// Test case 2
	693@param#array := 1
	694@param#diff := 0
	690@param#i := 1
	691@param#array:len := 0
    result := changeArrayValues(693@param#array, 694@param#diff, 690@param#i, 691@param#array:len)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_changeArrayValues2 failed")
	}
	
}

func test_DivBy() {

	// Test case 1
	701@param#invokeObject := 2
	702@param#den := 30
	699@param#i := 2
    result := DivBy(701@param#invokeObject, 702@param#den, 699@param#i)
	if result != 33, string!val!96784904 {
	    t.Errorf("test_DivBy1 failed")
	}
	
	// Test case 2
	702@param#den := 0
	701@param#invokeObject := 1
	699@param#i := 1
    result := DivBy(702@param#den, 701@param#invokeObject, 699@param#i)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_DivBy2 failed")
	}
	
}

func test_thirdLevelWithException() {

	// Test case 1
	703@param#i := 2
	705@param#invokeObject := 1
    result := thirdLevelWithException(703@param#i, 705@param#invokeObject)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_thirdLevelWithException1 failed")
	}
	
	// Test case 2
	703@param#i := 1
	705@param#invokeObject := 2
    result := thirdLevelWithException(703@param#i, 705@param#invokeObject)
	if result != G*NAMED:InvokeClass, string!val!96784904 {
	    t.Errorf("test_thirdLevelWithException2 failed")
	}
	
}

func test_secondLevelWithException() {

	// Test case 1
	708@param#invokeObject := 2
	706@param#i := 1
    result := secondLevelWithException(708@param#invokeObject, 706@param#i)
	if result != G*NAMED:InvokeClass, string {
	    t.Errorf("test_secondLevelWithException1 failed")
	}
	
	// Test case 2
	708@param#invokeObject := 1
	706@param#i := 1
    result := secondLevelWithException(708@param#invokeObject, 706@param#i)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_secondLevelWithException2 failed")
	}
	
}

func test_half() {

	// Test case 1
	709@param#i := 1
	710@param#a := 0
    result := half(709@param#i, 710@param#a)
	if result != 0 {
	    t.Errorf("test_half1 failed")
	}
	
}

func test_NestedMethodWithException() {

	// Test case 1
	713@param#invokeObject := 1
	711@param#i := 1
    result := NestedMethodWithException(713@param#invokeObject, 711@param#i)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_NestedMethodWithException1 failed")
	}
	
	// Test case 2
	713@param#invokeObject := 2
	711@param#i := 1
    result := NestedMethodWithException(713@param#invokeObject, 711@param#i)
	if result != G*NAMED:InvokeClass, string!val!96784904 {
	    t.Errorf("test_NestedMethodWithException2 failed")
	}
	
}

func test_NullAsParameter() {

	// Test case 1
	715@param#den := 0
	714@param#i := 1
    result := NullAsParameter(715@param#den, 714@param#i)
	if result != "something done with null" {
	    t.Errorf("test_NullAsParameter1 failed")
	}
	
}

func test_ConstraintsFromInside() {

	// Test case 1
	716@param#i := 1
	717@param#value := 0
    result := ConstraintsFromInside(716@param#i, 717@param#value)
	if result != 1 {
	    t.Errorf("test_ConstraintsFromInside1 failed")
	}
	
	// Test case 2
	716@param#i := 1
	717@param#value := -9223372036854775808
    result := ConstraintsFromInside(716@param#i, 717@param#value)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ConstraintsFromInside2 failed")
	}
	
	// Test case 3
	716@param#i := 1
	717@param#value := -1
    result := ConstraintsFromInside(716@param#i, 717@param#value)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ConstraintsFromInside3 failed")
	}
	
}

func test_GetNullOrValue() {

	// Test case 1
	718@param#i := 1
	720@param#invokeObject := 1
    result := GetNullOrValue(718@param#i, 720@param#invokeObject)
	if result != G*NAMED:InvokeClass {
	    t.Errorf("test_GetNullOrValue1 failed")
	}
	
	// Test case 2
	718@param#i := 1
	720@param#invokeObject := 1
    result := GetNullOrValue(718@param#i, 720@param#invokeObject)
	if result != nil {
	    t.Errorf("test_GetNullOrValue2 failed")
	}
	
}

func test_UpdateValue() {

	// Test case 1
	721@param#i := 1
	724@param#value := 143
	723@param#invokeObject := 1
    result := UpdateValue(721@param#i, 724@param#value, 723@param#invokeObject)
	if result != G*NAMED:InvokeClass, string!val!96784904 {
	    t.Errorf("test_UpdateValue1 failed")
	}
	
	// Test case 2
	721@param#i := 1
	724@param#value := 0
	723@param#invokeObject := 1
    result := UpdateValue(721@param#i, 724@param#value, 723@param#invokeObject)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_UpdateValue2 failed")
	}
	
	// Test case 3
	721@param#i := 1
	724@param#value := 0
	723@param#invokeObject := 1
    result := UpdateValue(721@param#i, 724@param#value, 723@param#invokeObject)
	if result != G*NAMED:InvokeClass, string!val!96784904 {
	    t.Errorf("test_UpdateValue3 failed")
	}
	
}

func test_ArrayCopyExample() {

	// Test case 1
	728@param#array := 2
	725@param#i := 1
	726@param#array:len := 9223372036854775709
    result := ArrayCopyExample(728@param#array, 725@param#i, 726@param#array:len)
	if result != nil, string!val!96784904 {
	    t.Errorf("test_ArrayCopyExample1 failed")
	}
	
	// Test case 2
	728@param#array := 2
	725@param#i := 1
	726@param#array:len := 0
    result := ArrayCopyExample(728@param#array, 725@param#i, 726@param#array:len)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_ArrayCopyExample2 failed")
	}
	
}

func test_ExceptionInNestedMethod() {

	// Test case 1
	736@param#value := -1
	733@param#i := 1
	735@param#invokeObject := 2
    result := ExceptionInNestedMethod(736@param#value, 733@param#i, 735@param#invokeObject)
	if result != G*INT64, nil, string {
	    t.Errorf("test_ExceptionInNestedMethod1 failed")
	}
	
}

func test_changeTwoObjects() {

	// Test case 1
	737@param#i := 1
	740@param#snd := 1
	739@param#fst := 1
    result := changeTwoObjects(737@param#i, 740@param#snd, 739@param#fst)
	if result !=  {
	    t.Errorf("test_changeTwoObjects1 failed")
	}
	
}

func test_changeAndReturnArray() {

	// Test case 1
	745@param#diff := -38
	742@param#array:len := 2
	741@param#i := 2
	744@param#array := 1
    result := changeAndReturnArray(745@param#diff, 742@param#array:len, 741@param#i, 744@param#array)
	if result != G*[INT64] {
	    t.Errorf("test_changeAndReturnArray1 failed")
	}
	
	// Test case 2
	745@param#diff := 2
	742@param#array:len := 0
	741@param#i := 1
	744@param#array := 2
    result := changeAndReturnArray(745@param#diff, 742@param#array:len, 741@param#i, 744@param#array)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_changeAndReturnArray2 failed")
	}
	
}

func test_ParticularValue() {

	// Test case 1
	751@param#invokeObject := 1
	749@param#i := 1
    result := ParticularValue(751@param#invokeObject, 749@param#i)
	if result != G*NAMED:InvokeClass, string!val!96784904 {
	    t.Errorf("test_ParticularValue1 failed")
	}
	
	// Test case 2
	751@param#invokeObject := 1
	749@param#i := 1
    result := ParticularValue(751@param#invokeObject, 749@param#i)
	if result != nil, string!val!-1565175336 {
	    t.Errorf("test_ParticularValue2 failed")
	}
	
}

func test_ChangeArrayByMethod() {

	// Test case 1
	753@param#array:len := 1
	752@param#i := 2
	755@param#array := 1
    result := ChangeArrayByMethod(753@param#array:len, 752@param#i, 755@param#array)
	if result != G*[INT64] {
	    t.Errorf("test_ChangeArrayByMethod1 failed")
	}
	
}

func test_CreateObjectFromValue() {

	// Test case 1
	760@param#i := 2
	761@param#value := 0
    result := CreateObjectFromValue(760@param#i, 761@param#value)
	if result != G*NAMED:InvokeClass {
	    t.Errorf("test_CreateObjectFromValue1 failed")
	}
	
	// Test case 2
	760@param#i := 1
	761@param#value := 1
    result := CreateObjectFromValue(760@param#i, 761@param#value)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_CreateObjectFromValue2 failed")
	}
	
}

func test_mult() {

	// Test case 1
	765@param#a := 0
	766@param#b := 0
	764@param#i := 1
    result := mult(765@param#a, 766@param#b, 764@param#i)
	if result != 0 {
	    t.Errorf("test_mult1 failed")
	}
	
}

func test_AlwaysNPE() {

	// Test case 1
	767@param#i := 1
	769@param#invokeObject := 1
    result := AlwaysNPE(767@param#i, 769@param#invokeObject)
	if result != "something done with null" {
	    t.Errorf("test_AlwaysNPE1 failed")
	}
	
	// Test case 2
	767@param#i := 1
	769@param#invokeObject := 2
    result := AlwaysNPE(767@param#i, 769@param#invokeObject)
	if result != "something done with null" {
	    t.Errorf("test_AlwaysNPE2 failed")
	}
	
	// Test case 3
	767@param#i := 1
	769@param#invokeObject := 1
    result := AlwaysNPE(767@param#i, 769@param#invokeObject)
	if result != "something done with null" {
	    t.Errorf("test_AlwaysNPE3 failed")
	}
	
}

func test_ConstraintsFromOutside() {

	// Test case 1
	771@param#value := -1
	770@param#i := 1
    result := ConstraintsFromOutside(771@param#value, 770@param#i)
	if result != 1, string!val!96784904 {
	    t.Errorf("test_ConstraintsFromOutside1 failed")
	}
	
}

func test_changeValue() {

	// Test case 1
	775@param#value := 1
	774@param#objectValue := 1
	772@param#i := 1
    result := changeValue(775@param#value, 774@param#objectValue, 772@param#i)
	if result !=  {
	    t.Errorf("test_changeValue1 failed")
	}
	
}

func test_FewNestedException() {

	// Test case 1
	776@param#i := 1
	779@param#value := 0
	778@param#invokeObject := 1
    result := FewNestedException(776@param#i, 779@param#value, 778@param#invokeObject)
	if result != G*INT64, nil, string {
	    t.Errorf("test_FewNestedException1 failed")
	}
	
}

func test_ChangeArrayWithAssignFromMethod() {

	// Test case 1
	780@param#i := 1
	781@param#array:len := 6154759883197318126
	783@param#array := 2
    result := ChangeArrayWithAssignFromMethod(780@param#i, 781@param#array:len, 783@param#array)
	if result != G*[INT64] {
	    t.Errorf("test_ChangeArrayWithAssignFromMethod1 failed")
	}
	
}

func test_UpdateValues() {

	// Test case 1
	787@param#i := 1
	790@param#snd := 1
	789@param#fst := 2
    result := UpdateValues(787@param#i, 790@param#snd, 789@param#fst)
	if result != 1, string!val!96784904 {
	    t.Errorf("test_UpdateValues1 failed")
	}
	
	// Test case 2
	787@param#i := 1
	790@param#snd := 1
	789@param#fst := 1
    result := UpdateValues(787@param#i, 790@param#snd, 789@param#fst)
	if result != 0, string!val!-416009319 {
	    t.Errorf("test_UpdateValues2 failed")
	}
	
}

func test_ChangeObjectValueByMethod() {

	// Test case 1
	791@param#i := 1
	793@param#objectValue := 1
    result := ChangeObjectValueByMethod(791@param#i, 793@param#objectValue)
	if result != G*NAMED:InvokeClass {
	    t.Errorf("test_ChangeObjectValueByMethod1 failed")
	}
	
}

func test_SimpleFormula() {

	// Test case 1
	796@param#snd := 4611686018427387906
	794@param#i := 1
	795@param#fst := 128
    result := SimpleFormula(796@param#snd, 794@param#i, 795@param#fst)
	if result != -6917529027641081723, string!val!96784904 {
	    t.Errorf("test_SimpleFormula1 failed")
	}
	
	// Test case 2
	796@param#snd := 8
	794@param#i := 2
	795@param#fst := 152082971030716800
    result := SimpleFormula(796@param#snd, 794@param#i, 795@param#fst)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_SimpleFormula2 failed")
	}
	
	// Test case 3
	796@param#snd := 0
	794@param#i := 1
	795@param#fst := -1
    result := SimpleFormula(796@param#snd, 794@param#i, 795@param#fst)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_SimpleFormula3 failed")
	}
	
}

func main() {
	test_changeArrayValues()
	test_DivBy()
	test_thirdLevelWithException()
	test_secondLevelWithException()
	test_half()
	test_NestedMethodWithException()
	test_NullAsParameter()
	test_ConstraintsFromInside()
	test_GetNullOrValue()
	test_UpdateValue()
	test_ArrayCopyExample()
	test_ExceptionInNestedMethod()
	test_changeTwoObjects()
	test_changeAndReturnArray()
	test_ParticularValue()
	test_ChangeArrayByMethod()
	test_CreateObjectFromValue()
	test_mult()
	test_AlwaysNPE()
	test_ConstraintsFromOutside()
	test_changeValue()
	test_FewNestedException()
	test_ChangeArrayWithAssignFromMethod()
	test_UpdateValues()
	test_ChangeObjectValueByMethod()
	test_SimpleFormula()
}

// Original code:


type InvokeClass struct {
	Value int
}

func (i *InvokeClass) DivBy(den int) int {
	return i.Value / den
}

func (i *InvokeClass) UpdateValue(newValue int) {
	i.Value = newValue
}

type InvokeExample struct{}

func (i *InvokeExample) mult(a, b int) int {
	return a * b
}

func (i *InvokeExample) half(a int) int {
	return a / 2
}

func (i *InvokeExample) SimpleFormula(fst, snd int) (int, error) {
	if fst < 100 {
		return 0, errors.New("IllegalArgumentException: fst < 100")
	} else if snd < 100 {
		return 0, errors.New("IllegalArgumentException: snd < 100")
	}

	x := fst + 5
	y := i.half(snd)

	return i.mult(x, y), nil
}

func (i *InvokeExample) initialize(value int) *InvokeClass {
	objectValue := &InvokeClass{
		Value: value,
	}
	return objectValue
}

func (i *InvokeExample) CreateObjectFromValue(value int) *InvokeClass {
	if value == 0 {
		value = 1
	}
	return i.initialize(value)
}

func (i *InvokeExample) changeValue(objectValue *InvokeClass, value int) {
	objectValue.Value = value
}

func (i *InvokeExample) ChangeObjectValueByMethod(objectValue *InvokeClass) *InvokeClass {
	objectValue.Value = 1
	i.changeValue(objectValue, 4)
	return objectValue
}

func (i *InvokeExample) getFive() int {
	return 5
}

func (i *InvokeExample) getTwo() int {
	return 2
}

func (i *InvokeExample) ParticularValue(invokeObject *InvokeClass) (*InvokeClass, error) {
	if invokeObject.Value < 0 {
		return nil, errors.New("IllegalArgumentException: value < 0")
	}
	x := i.getFive() * i.getTwo()
	y := i.getFive() / i.getTwo()

	invokeObject.Value = x + y
	return invokeObject, nil
}

func (i *InvokeExample) getNull() *InvokeClass {
	return nil
}

func (i *InvokeExample) GetNullOrValue(invokeObject *InvokeClass) *InvokeClass {
	if invokeObject.Value < 100 {
		return i.getNull()
	}
	invokeObject.Value = i.getFive()
	return invokeObject
}

func (i *InvokeExample) abs(value int) int {
	if value < 0 {
		if value == math.MinInt {
			return 0
		}
		return i.mult(-1, value)
	}
	return value
}

func (i *InvokeExample) ConstraintsFromOutside(value int) (int, error) {
	if i.abs(value) < 0 {
		return 0, errors.New("IllegalArgumentException: abs(value) < 0")
	}
	return i.abs(value), nil
}

func (i *InvokeExample) helper(value int) int {
	if value < 0 {
		return -1
	}
	return 1
}

func (i *InvokeExample) ConstraintsFromInside(value int) int {
	if value < 0 {
		if value == math.MinInt {
			value = 0
		} else {
			value = -value
		}
	}
	return i.helper(value)
}

func (i *InvokeExample) AlwaysNPE(invokeObject *InvokeClass) *InvokeClass {
	if invokeObject.Value == 0 {
		invokeObject = i.getNull()
		invokeObject.Value = 0
		return invokeObject
	} else if invokeObject.Value > 0 {
		invokeObject = i.getNull()
		invokeObject.Value = 1
		return invokeObject
	} else {
		invokeObject = i.getNull()
		invokeObject.Value = -1
		return invokeObject
	}
}

func (i *InvokeExample) NestedMethodWithException(invokeObject *InvokeClass) (*InvokeClass, error) {
	if invokeObject.Value < 0 {
		return nil, errors.New("IllegalArgumentException: value < 0")
	}
	return invokeObject, nil
}

func (i *InvokeExample) ExceptionInNestedMethod(invokeObject *InvokeClass, value int) (*InvokeClass, error) {
	invokeObject.Value = value
	return i.NestedMethodWithException(invokeObject)
}

func (i *InvokeExample) FewNestedException(invokeObject *InvokeClass, value int) (*InvokeClass, error) {
	invokeObject.Value = value
	return i.firstLevelWithException(invokeObject)
}

func (i *InvokeExample) firstLevelWithException(invokeObject *InvokeClass) (*InvokeClass, error) {
	if invokeObject.Value < 10 {
		return nil, errors.New("IllegalArgumentException: value < 10")
	}
	return i.secondLevelWithException(invokeObject)
}

func (i *InvokeExample) secondLevelWithException(invokeObject *InvokeClass) (*InvokeClass, error) {
	if invokeObject.Value < 100 {
		return nil, errors.New("IllegalArgumentException: value < 100")
	}
	return i.thirdLevelWithException(invokeObject)
}

func (i *InvokeExample) thirdLevelWithException(invokeObject *InvokeClass) (*InvokeClass, error) {
	if invokeObject.Value < 10000 {
		return nil, errors.New("IllegalArgumentException: value < 10000")
	}
	return invokeObject, nil
}

func (i *InvokeExample) DivBy(invokeObject *InvokeClass, den int) (int, error) {
	if invokeObject.Value < 1000 {
		return 0, errors.New("IllegalArgumentException: value < 1000")
	}
	return invokeObject.DivBy(den), nil
}

func (i *InvokeExample) UpdateValue(invokeObject *InvokeClass, value int) (*InvokeClass, error) {
	if invokeObject.Value > 0 {
		return invokeObject, nil
	}
	if value > 0 {
		invokeObject.UpdateValue(value)
		if invokeObject.Value != value {
			return nil, errors.New("RuntimeException: unreachable branch")
		}
		return invokeObject, nil
	}
	return nil, errors.New("IllegalArgumentException")
}

func (i *InvokeExample) NullAsParameter(den int) (int, error) {
	return i.DivBy(nil, den)
}

func (i *InvokeExample) ChangeArrayWithAssignFromMethod(array []int) []int {
	return i.changeAndReturnArray(array, 5)
}

func (i *InvokeExample) changeAndReturnArray(array []int, diff int) []int {
	updatedArray := make([]int, len(array))
	for idx := range array {
		updatedArray[idx] = array[idx] + diff
	}
	return updatedArray
}

func (i *InvokeExample) ChangeArrayByMethod(array []int) []int {
	i.changeArrayValues(array, 5)
	return array
}

func (i *InvokeExample) changeArrayValues(array []int, diff int) {
	for idx := range array {
		array[idx] += diff
	}
}

func (i *InvokeExample) ArrayCopyExample(array []int) ([]int, error) {
	if len(array) < 3 {
		return nil, errors.New("IllegalArgumentException: array length < 3")
	}

	if array[0] <= array[1] || array[1] <= array[2] {
		return nil, nil
	}

	dst := make([]int, len(array))
	i.arrayCopy(array, 0, dst, 0, len(array))

	return dst, nil
}

func (i *InvokeExample) arrayCopy(src []int, srcPos int, dst []int, dstPos int, length int) {
	for j := 0; j < length; j++ {
		dst[dstPos+j] = src[srcPos+j]
	}
}

func (i *InvokeExample) UpdateValues(fst *InvokeClass, snd *InvokeClass) (int, error) {
	i.changeTwoObjects(fst, snd)
	if fst.Value == 1 && snd.Value == 2 {
		return 1, nil
	}
	return 0, errors.New("RuntimeException: values do not match expected output")
}

func (i *InvokeExample) changeTwoObjects(fst *InvokeClass, snd *InvokeClass) {
	fst.Value = 1
	snd.Value = 2
}

