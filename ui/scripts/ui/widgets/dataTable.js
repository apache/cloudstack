// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
(function($) {
    /**
     * Convert table to be resizable and sortable
     *
     */
    $.fn.dataTable = function(method, options) {
        var $table = this;

        /**
         * Check if position is in 'resize zone'
         *
         * @return boolean, true if position is within bounds
         */
        var withinResizeBounds = function($elem, posX) {
            var leftBound = $elem.offset().left + $elem.width() / 1.2;

            return posX > leftBound;
        };

        /**
         * Handles actual resizing of table headers
         */
        var resizeDragEvent = function(event) {
            var $elem = $(this);

            if (event.type == 'mousedown') {
                $elem.addClass('dragging');

                return false;
            } else if (event.type == 'mouseup') {
                $table.find('th').removeClass('dragging');

                return false;
            }

            var isDraggable = $elem.hasClass('dragging');

            if (!isDraggable) {
                return false;
            }

            var columnIndex = $elem.index();

            // Get all TDs from column
            var columnCells = [];
            $table.find('tbody tr:first').each(function() {
                var targetCell = $($(this).find('td')[columnIndex]);

                columnCells.push(targetCell);
            });

            var tolerance = 25;
            var targetWidth = event.pageX - $elem.offset().left + tolerance;
            $(columnCells).each(function() {
                $(this).css({
                    width: targetWidth
                });
            });

            resizeHeaders();

            return true;
        };

        var reattachTable = function() {
            var $mainContainer = $('<div>')
                .addClass('data-table')
                .appendTo($table.parent())
                .append($table.detach()
            );
        };

        /**
         * Event to set resizable appearance on hover
         */
        var hoverResizableEvent = function(event) {
            var $elem = $(this);
            var posX = event.pageX;

            if (event.type != 'mouseout' && withinResizeBounds($elem, posX)) {
                $elem.addClass('resizable');
            } else {
                $elem.removeClass('resizable');
            }

            return true;
        };

        /**
         * Make row at specified index selected or unselected
         *
         * @param rowIndex Row's index, starting at 1
         */
        var toggleSelectRow = function(rowIndex) {
            var $rows = $table.find('tbody tr');
            var $row = $($rows[rowIndex]);

            $row.siblings().removeClass('selected');
            return $row.addClass('selected');
        };

        var computeEvenOddRows = function() {
            var currentRowType = 'even';
            $table.find('tbody tr').each(function() {
                var $row = $(this);

                $row.removeClass('even').removeClass('odd');
                $row.addClass(currentRowType);

                if (currentRowType == 'even') currentRowType = 'odd';
                else currentRowType = 'even';
            });
        };

        /**
         * Sort table by column
         *
         * @param columnIndex Index of column (starting at 0) to sort by
         */
        var sortTable = function(columnIndex) {
            var direction = 'asc';

            if ($table.find('thead tr:last th').hasClass('sorted ' + direction)) {
                direction = 'desc';
            }

            $table.find('thead tr:last th').removeClass('sorted desc asc');
            $($table.find('thead tr:last th')[columnIndex]).addClass('sorted').addClass(direction);

            var $elems = $table.find('tbody td').filter(function() {
                return $(this).index() == columnIndex;
            });

            if ($elems.length < 2) {
                return;
            }
            // strict function for filtering numbers (e.g. "2.3", "-2" but not "8 CPUs")
            var filterNumber = function (value) {
              if(/^[-+]?\d*\.?\d*$/.test(value))
                return Number(value);
              return NaN;
            };
            var stringComparator = function(a,b) {
                return a.html().localeCompare(b.html());
            };
            var numericComparator = function(a,b) {
                return filterNumber(a.children().html()) < filterNumber(b.children().html()) ? 1 : -1;
            };
            var stateComparator = function(a,b) {
                return a.attr('title').localeCompare(b.attr('title'));
            };
            var ipV4AddressCIDRComparator = function(a,b) {
                a = a.children().html().split(/[./]/gm);
                b = b.children().html().split(/[./]/gm);
                for( var i = 0; i < a.length; i++ )
                {
                  if( ( a[i] = parseInt( a[i] ) ) < ( b[i] = parseInt( b[i] ) ) )
                    return -1;
                  else if( a[i] > b[i] )
                    return 1;
                }
                return 0;
            };
            var ipV6AddressCIDRComparator = function(a,b) {
              a = a.children().html().split(/[:/]/gm);
              b = b.children().html().split(/[:/]/gm);
              for( var i = 0; i < a.length; i++ )
              {
                if((a[i] = parseInt("0x" + a[i] , 16)) < ( b[i] = parseInt( "0x" + b[i], 16)))
                  return -1;
                else if( a[i] > b[i] )
                  return 1;
              }
              return 0;
            };
            var isIpV4Address = function(obj) {
              return !$.isArray(obj) && (/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/gm).test(obj);
            };
            var isIpV6Address = function(obj) {
              return !$.isArray(obj) && (/^[a-fA-F0-9:]+$/gm).test(obj);
            };
            var isIpV4CIDRAddress = function(obj) {
              return !$.isArray(obj) && (/^([0-9]{1,3}\.){3}[0-9]{1,3}(\/([0-9]|[1-2][0-9]|3[0-2]))?$/gm).test(obj);
            };
            var isIpV6CIDRAddress = function(obj) {
              return !$.isArray(obj) && (/^s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*(\/([0-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8]))?$/gm).test(obj);
            };

            var isNumeric = function(obj) {
                return !$.isArray(obj) && !isNaN(filterNumber(obj));
            };

            var comparator = stringComparator;
            var hasAllRowsSameValue = true;
            var firstElem = $($elems[0]).html();
            var sortData = [];
            var numericDataCount = 0;
            var ipV4AddressDataCount = 0;
            var ipV4CIDRDataCount = 0;
            var ipV6AddressDataCount = 0;
            var ipV6CIDRDataCount = 0;
            $elems.each(function() {
                var text = $(this);
                if (hasAllRowsSameValue) {
                    if (firstElem !== text.html()) {
                        hasAllRowsSameValue = false;
                    }
                }
                if (text.children()) {
                    text = text.children().html();
                } else {
                    text = text.html();
                }
                if (isIpV4CIDRAddress(text) || !text){
                    ipV4CIDRDataCount += 1;
                }
                if (isIpV4Address(text) || !text){
                    ipV4AddressDataCount += 1;
                }
                if (isIpV6Address(text) || !text){
                    ipV6AddressDataCount += 1;
                }
                if (isIpV6CIDRAddress(text) || !text){
                    ipV6CIDRDataCount += 1;
                }
                if (isNumeric(text) || !text) {
                    numericDataCount += 1;
                }
                sortData.push($(this));
            });

            if ($($elems[0]).hasClass('state')) {
                comparator = stateComparator;
            } else {
                if (hasAllRowsSameValue) {
                    return;
                }
                if (columnIndex !== 0){
                  var relevantElementsBorder = $elems.length / 4;
                  if (numericDataCount > relevantElementsBorder) {
                    comparator = numericComparator;
                  }
                  if (ipV4AddressDataCount + ipV4CIDRDataCount > relevantElementsBorder){
                    comparator = ipV4AddressCIDRComparator;
                  }
                  if (ipV6AddressDataCount + ipV6AddressDataCount > relevantElementsBorder){
                    comparator = ipV6AddressCIDRComparator;
                  }
                }

            }

            sortData.sort(comparator);

            if (direction == 'asc') {
                sortData.reverse();
            }

            var elements = [];
            $(sortData).each(function() {
                elements.push($(this).parent().clone(true));
            });

            var $tbody = $table.find('tbody');
            $tbody.empty();
            $(elements).each(function() {
                $(this).appendTo($tbody);
            });

            computeEvenOddRows();
        };

        var resizeHeaders = function() {
            var $thead = $table.hasClass('no-split') ? $table.find('thead') : $table.closest('div.data-table').find('thead');
            var $tbody = $table.find('tbody');
            var $ths = $thead.find('th');
            var $tds = $tbody.find('tr:first td');

            if ($table.hasClass('no-split')) {
                $tbody.width($thead.width());
            }

            if ($ths.length > $tds.length) {
                $ths.width(
                    $table.width() / $ths.length
                );
                return false;
            }

            $ths.each(function() {
                var $th = $(this);

                if ($th.hasClass('collapsible-column')) {
                    return true;
                }

                var $td = $tds.filter(function() {
                    return $(this).index() == $th.index();
                });

                $th.width($td.width());

                return true;
            });

            return $ths;
        };

        var methods = {
            removeRow: function(rowIndex) {
                var $row = $($table.find('tbody tr')[rowIndex]);

                $row.fadeOut(function() {
                    $row.remove();
                    computeEvenOddRows();
                });

                return $row;
            },

            refresh: function() {
                resizeHeaders();
                computeEvenOddRows();
            },

            selectRow: function(rowIndex) {
                var $row = $($table.find('tbody tr')[rowIndex]);

                $row.siblings().removeClass('selected');
                $row.addClass('selected');
            }
        };

        var init = function() {
            var noSelect = options && options.noSelect == true ? true : false;
            if (!$table.closest('div.data-table').length && !$table.hasClass('no-split')) {
                reattachTable();
                $table.find('tbody').closest('table').addClass('body');
            }

            if (!$table.hasClass('horizontal-overflow')) {
                $table.find('th:not(:has(input))').bind('mousemove mouseout', hoverResizableEvent);
                $table.find('th:not(:has(input))').bind('mousedown mousemove mouseup mouseout', resizeDragEvent);
            }

            $table.find('thead tr:last th:not(:has(input)):not(.collapsible-column):not(.quick-view)').unbind('click').bind('click', function(event) {
                if ($(this).hasClass('resizable')) {
                    return false;
                }

                sortTable($(event.target).index());

                return false;
            });

            $table.bind('click', function(event) {
                var $tr = $(event.target).closest('tr');

                if (!$tr.length || noSelect) return true;
                var rowIndex = $tr.index();

                toggleSelectRow(rowIndex);

                return true;
            });

            computeEvenOddRows();
            resizeHeaders();
        };

        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (!method) {
            init();
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.dataTable');
        }

        return $table;
    };
}(jQuery));
