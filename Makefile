check :
	pre-commit run --all-files
.PHONY : check

checkinstall :
	pre-commit install
.PHONY : checkinstall

checkupdate :
	pre-commit autoupdate
.PHONY : checkupdate
