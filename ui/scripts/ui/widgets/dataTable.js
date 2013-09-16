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

        var splitTable = function() {
            var $mainContainer = $('<div>')
                .addClass('data-table')
                .appendTo($table.parent())
                .append(
                    $table.remove()
            );
            $table = $mainContainer;
            var $theadContainer = $('<div>').addClass('fixed-header').prependTo($table);
            var $theadTable = $('<table>').appendTo($theadContainer).attr('nowrap', 'nowrap');
            var $thead = $table.find('thead').remove().appendTo($theadTable);

            return $thead;
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
            return false;
            var direction = 'asc';

            if ($table.find('thead th').hasClass('sorted ' + direction)) {
                direction = 'desc';
            }

            $table.find('thead th').removeClass('sorted desc asc');
            $($table.find('thead th')[columnIndex]).addClass('sorted').addClass(direction);

            var $elems = $table.find('tbody td').filter(function() {
                return $(this).index() == columnIndex;
            });

            var sortData = [];
            $elems.each(function() {
                sortData.push($(this).html());
                sortData.sort();

                if (direction == 'asc') {
                    sortData.reverse();
                }
            });

            $(sortData).each(function() {
                var sortKey = this;
                var $targetCell = $elems.filter(function() {
                    return $(this).html() == sortKey;
                });
                var $targetContainer = $targetCell.parent();

                $targetContainer.remove().appendTo($table.find('tbody'));
            });

            computeEvenOddRows();
        };

        var resizeHeaders = function() {
            var $thead = $table.closest('div.data-table').find('thead');
            var $tbody = $table.find('tbody');
            var $ths = $thead.find('th');
            var $tds = $tbody.find('tr:first td');

            if ($ths.size() > $tds.size()) {
                $ths.width(
                    $table.width() / $ths.size()
                );
                return false;
            }

            $ths.each(function() {
                var $th = $(this);

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
            if (!$table.closest('div.data-table').size() && !$table.hasClass('no-split')) {
                splitTable();
                $table.find('tbody').closest('table').addClass('body');
            }

            $table.find('th').bind('mousemove mouseout', hoverResizableEvent);
            $table.find('th').bind('mousedown mousemove mouseup mouseout', resizeDragEvent);
            $table.find('th').bind('click', function(event) {
                if ($(this).hasClass('resizable')) {
                    return false;
                }

                sortTable($(event.target).index());

                return false;
            });

            $table.bind('click', function(event) {
                var $tr = $(event.target).closest('tr');

                if (!$tr.size() || noSelect) return true;
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
